package com.campushub.shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campushub.common.exception.BusinessException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.infrastructure.security.AuthenticationService;
import com.campushub.shop.dto.UserProductCreateRequest;
import com.campushub.shop.dto.UserProductQueryRequest;
import com.campushub.shop.entity.Product;
import com.campushub.shop.entity.UserProduct;
import com.campushub.shop.mapper.ProductMapper;
import com.campushub.shop.mapper.UserProductMapper;
import com.campushub.shop.service.UserProductService;
import com.campushub.shop.vo.PageVO;
import com.campushub.shop.vo.UserProductVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** 用户购买商品基础业务实现。 */
@Service
@RequiredArgsConstructor
public class UserProductServiceImpl implements UserProductService {

    private final UserProductMapper userProductMapper;

    private final ProductMapper productMapper;

    private final AuthenticationService authenticationService;

    /** 游标分页默认页大小。 */
    private static final int DEFAULT_PAGE_SIZE = 20;

    /** 游标分页最大页大小。 */
    private static final int MAX_PAGE_SIZE = 100;

    /** 商品上架状态。 */
    private static final int PRODUCT_STATUS_ON_SHELF = 1;

    /** 购买记录初始订单状态：待付款。 */
    private static final int ORDER_STATUS_PENDING_PAYMENT = 0;

    /**
     * 功能：下单，先按幂等键落库购买记录，再通过条件更新扣减库存，整笔操作在同一事务内完成。
     *
     * <p>插入放在扣库存之前，是为了让唯一索引先拦住重复提交：同一幂等键的重复请求直接命中
     * 唯一键冲突，回查原记录返回，库存不会被扣第二次。库存扣减使用 product_stock >= 购买数量
     * 的条件更新保证并发正确性，影响行数为 0 说明库存已被并发请求抢空，抛 409 并回滚本笔
     * 刚插入的购买记录，避免留下没有库存支撑的订单。
     *
     * @param request 下单请求，包含商品 ID、购买数量、可选预约付款时间与前端生成的幂等键
     * @return 创建成功的购买记录；重复提交时返回首次落库的原记录
     * @throws BusinessException 商品不存在抛 404，商品已下架或库存不足抛 409，登录失效抛 401
     */
    @Override
    @Transactional
    public UserProductVO create(UserProductCreateRequest request) {
        Long userId = requireCurrentUserId();
        String idempotencyKey = request.userProductIdempotencyKey().trim();
        Product product = requireProduct(request.productId());
        if (product.getProductStatus() != PRODUCT_STATUS_ON_SHELF) {
            throw new BusinessException(ErrorCode.CONFLICT, "商品已下架");
        }
        UserProduct userProduct = new UserProduct();
        userProduct.setUserId(userId);
        userProduct.setProductId(product.getProductId());
        userProduct.setProductNumber(request.productNumber());
        userProduct.setPurchaseTime(LocalDateTime.now());
        userProduct.setPaymentTime(request.paymentTime());
        userProduct.setOrderStatus(ORDER_STATUS_PENDING_PAYMENT);
        userProduct.setUserProductIdempotencyKey(idempotencyKey);
        try {
            userProductMapper.insert(userProduct);
        } catch (DuplicateKeyException exception) {
            // 唯一键冲突说明同一幂等键已下单成功，直接返回原记录，此时尚未扣库存，不会重复扣减
            UserProduct existingRecord = userProductMapper.selectOne(Wrappers.<UserProduct>lambdaQuery()
                    .eq(UserProduct::getUserId, userId)
                    .eq(UserProduct::getUserProductIdempotencyKey, idempotencyKey)
                    .last("LIMIT 1"));
            if (existingRecord == null) {
                throw new BusinessException(ErrorCode.CONFLICT, "下单幂等键已被使用");
            }
            return toUserProductVO(existingRecord);
        }
        // 记录已占位再扣库存：影响行数 0 时抛 409，由事务连带回滚上面刚插入的购买记录
        int deducted = productMapper.deductStock(product.getProductId(), request.productNumber());
        if (deducted == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "商品库存不足");
        }
        return getById(userProduct.getUserProductId());
    }

    /** 根据记录 ID 查询购买记录，记录不存在时返回 404 业务异常。 */
    @Override
    public UserProductVO getById(Long userProductId) {
        return toUserProductVO(requireUserProduct(userProductId));
    }

    /** 游标分页查询当前登录用户自己的购买记录，按创建时间倒序。 */
    @Override
    public PageVO<UserProductVO> query(UserProductQueryRequest request) {
        Long userId = requireCurrentUserId();
        int pageSize = resolvePageSize(request.pageSize());
        LambdaQueryWrapper<UserProduct> wrapper = Wrappers.<UserProduct>lambdaQuery()
                .eq(UserProduct::getUserId, userId);
        applyCursor(wrapper, request.cursorTime(), request.cursorId());
        wrapper.orderByDesc(UserProduct::getCreateTime).orderByDesc(UserProduct::getUserProductId)
                .last("LIMIT " + pageSize);
        List<UserProduct> records = userProductMapper.selectList(wrapper);
        List<UserProductVO> list = records.stream().map(this::toUserProductVO).toList();
        if (list.size() < pageSize) {
            return new PageVO<>(list, false, null, null);
        }
        // 取满一页说明可能还有更多数据，最后一行即下一页游标
        UserProduct lastRecord = records.get(records.size() - 1);
        return new PageVO<>(list, true, lastRecord.getCreateTime(), lastRecord.getUserProductId());
    }

    /** 物理删除当前登录用户自己的购买记录。 */
    @Override
    public void delete(Long userProductId) {
        UserProduct userProduct = requireUserProduct(userProductId);
        Long currentUserId = requireCurrentUserId();
        if (!currentUserId.equals(userProduct.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能删除自己的购买记录");
        }
        userProductMapper.deleteById(userProduct.getUserProductId());
    }

    /** 根据记录 ID 查询购买记录，记录不存在时抛出 404 业务异常。 */
    private UserProduct requireUserProduct(Long userProductId) {
        if (userProductId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "购买记录 ID 不能为空");
        }
        UserProduct userProduct = userProductMapper.selectById(userProductId);
        if (userProduct == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "购买记录不存在");
        }
        return userProduct;
    }

    /** 根据商品 ID 查询未删除商品，商品不存在时抛出 404 业务异常。 */
    private Product requireProduct(Long productId) {
        if (productId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "商品 ID 不能为空");
        }
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "商品不存在");
        }
        return product;
    }

    /** 页大小缺省 20，并限制在 1 到 100 之间。 */
    private int resolvePageSize(Integer pageSize) {
        if (pageSize == null) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
    }

    /** 获取当前登录用户 ID，登录状态失效时抛出 401 业务异常。 */
    private Long requireCurrentUserId() {
        Long userId = authenticationService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录状态已失效");
        }
        return userId;
    }

    /** 将购买记录实体转换为接口返回对象。 */
    private UserProductVO toUserProductVO(UserProduct userProduct) {
        return new UserProductVO(
                userProduct.getUserProductId(),
                userProduct.getUserId(),
                userProduct.getProductId(),
                userProduct.getProductNumber(),
                userProduct.getPurchaseTime(),
                userProduct.getPaymentTime(),
                userProduct.getOrderStatus(),
                userProduct.getUserProductIdempotencyKey(),
                userProduct.getCreateTime()
        );
    }

    /** 追加游标条件：按 (create_time, user_product_id) 倒序定位上一页末行之后。 */
    private void applyCursor(LambdaQueryWrapper<UserProduct> wrapper, LocalDateTime cursorTime, Long cursorId) {
        if (cursorTime == null || cursorId == null) {
            return;
        }
        wrapper.and(w -> w.lt(UserProduct::getCreateTime, cursorTime)
                .or(o -> o.eq(UserProduct::getCreateTime, cursorTime).lt(UserProduct::getUserProductId, cursorId)));
    }
}
