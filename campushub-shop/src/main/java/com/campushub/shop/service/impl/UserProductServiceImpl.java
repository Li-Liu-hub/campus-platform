package com.campushub.shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campushub.common.exception.BusinessException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.infrastructure.security.AuthenticationService;
import com.campushub.shop.constant.ProductStatuses;
import com.campushub.shop.constant.PurchaseStatuses;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** 用户购买商品基础业务实现。 */
@Slf4j
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

    /**
     * 功能：下单，先按幂等键落库待付款记录，再冻结商品库存，整笔操作在同一事务内完成。
     *
     * <p>「下单」的库存语义是<b>冻结</b>而不是扣减：可卖量减少的同时等量记入冻结量，
     * 货还没卖出去，只是被这笔记账占住。付款时消耗冻结量，取消或超时时解冻回补。
     *
     * <p>插入放在冻结之前，是为了让唯一索引先拦住重复提交：同一幂等键的重复请求直接命中
     * 唯一键冲突，回查原记录返回，此时尚未冻结任何库存，天然不会重复冻结。冻结使用
     * product_stock >= 购买数量的条件更新保证并发正确性，影响行数为 0 说明可卖量已被
     * 并发请求抢空，抛 409 并回滚本笔刚插入的记录，避免留下没有库存支撑的购买记录。
     *
     * @param request 下单请求，包含商品 ID、购买数量与前端生成的幂等键
     * @return 创建成功的待付款购买记录；重复提交时返回首次落库的原记录
     * @throws BusinessException 商品不存在抛 404，商品已下架或可卖量不足抛 409，登录失效抛 401
     */
    @Override
    @Transactional
    public UserProductVO create(UserProductCreateRequest request) {
        Long userId = requireCurrentUserId();
        String idempotencyKey = request.userProductIdempotencyKey().trim();
        Product product = requireProduct(request.productId());
        if (product.getProductStatus() != ProductStatuses.ON_SHELF) {
            throw new BusinessException(ErrorCode.CONFLICT, "商品已下架");
        }
        UserProduct userProduct = new UserProduct();
        userProduct.setUserId(userId);
        userProduct.setProductId(product.getProductId());
        userProduct.setProductNumber(request.productNumber());
        userProduct.setPurchaseTime(LocalDateTime.now());
        userProduct.setOrderStatus(PurchaseStatuses.PENDING_PAYMENT);
        userProduct.setUserProductIdempotencyKey(idempotencyKey);
        try {
            userProductMapper.insert(userProduct);
        } catch (DuplicateKeyException exception) {
            // 唯一键冲突说明同一幂等键已下单成功，直接返回原记录，此时尚未冻结库存，不会重复冻结
            // 锁定读（当前读）：普通 SELECT 受 REPEATABLE READ 快照限制，可能读不到并发赢家刚提交的记录而误报 409；
            // FOR SHARE 与唯一键冲突残留的 S 锁兼容，多个并发输家不会互相升级成 X 锁死锁
            UserProduct existingRecord = userProductMapper.selectOne(Wrappers.<UserProduct>lambdaQuery()
                    .eq(UserProduct::getUserId, userId)
                    .eq(UserProduct::getUserProductIdempotencyKey, idempotencyKey)
                    .last("LIMIT 1 FOR SHARE"));
            if (existingRecord == null) {
                throw new BusinessException(ErrorCode.CONFLICT, "下单幂等键已被使用");
            }
            return toUserProductVO(existingRecord);
        }
        // 记录已占位再冻结库存：影响行数 0 时抛 409，由事务连带回滚上面刚插入的购买记录
        int frozen = productMapper.freezeStock(product.getProductId(), request.productNumber());
        if (frozen == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "商品库存不足");
        }
        return getById(userProduct.getUserProductId());
    }

    /**
     * 功能：买家付款，把待付款记录置为已付款并把占用的冻结库存真正消耗掉。
     *
     * <p>两步顺序不可调换：<b>先抢占记录状态，抢到了才允许动库存。</b>
     * 状态条件是并发胜负的唯一判据——与超时任务同时发生时，只有一方能把 order_status
     * 从 0 改走，另一方影响行数为 0 并在此处直接拒绝，绝不会出现"状态没抢到却扣了库存"。
     *
     * <p>付款与取消的分水岭在库存那一步：付款只减冻结量、<b>不回补可卖量</b>，
     * 因为货真的卖掉了，这是消耗不是归还；取消则是冻结量减、可卖量加。
     *
     * <p>付款时刻取自应用时钟（LocalDateTime.now()）并显式写入，与同一行的
     * purchase_time 同源，避免数据库容器时区与 JVM 不一致时出现付款早于下单。
     *
     * @param userProductId 购买记录 ID，必须属于当前登录买家
     * @return 付款后的购买记录
     * @throws BusinessException 记录不存在抛 404，不属于本人或已非待付款抛 409，
     *                           冻结量与购买数量对不上（数据已不一致）抛 500 并回滚状态变更
     */
    @Override
    @Transactional
    public UserProductVO pay(Long userProductId) {
        Long userId = requireCurrentUserId();
        UserProduct userProduct = requireUserProduct(userProductId);
        int taken = userProductMapper.markPaidIfPending(userProductId, userId, LocalDateTime.now());
        if (taken == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "该购买记录不属于当前用户，或已付款、已取消");
        }
        int consumed = productMapper.consumeLockedStock(userProduct.getProductId(), userProduct.getProductNumber());
        if (consumed == 0) {
            // 抢到状态却消耗不掉冻结量，说明库存账目已不一致，必须回滚状态变更并告警
            log.error("付款消耗冻结库存失败，冻结量与购买数量不一致，userProductId={}，productId={}，number={}",
                    userProductId, userProduct.getProductId(), userProduct.getProductNumber());
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        return getById(userProductId);
    }

    /**
     * 功能：买家主动取消，把待付款记录置为已取消，并把占用的冻结库存回补到可卖量。
     *
     * <p>与付款同样是"先抢状态、抢到才动库存"：已付款的记录状态抢占必然失败，
     * 因此买家无法自行取消一笔已经付过款的交易。
     *
     * @param userProductId 购买记录 ID，必须属于当前登录买家
     * @return 取消后的购买记录
     * @throws BusinessException 记录不存在抛 404，不属于本人或已非待付款抛 409，
     *                           冻结量与购买数量对不上（数据已不一致）抛 500 并回滚状态变更
     */
    @Override
    @Transactional
    public UserProductVO cancel(Long userProductId) {
        Long userId = requireCurrentUserId();
        UserProduct userProduct = requireUserProduct(userProductId);
        int taken = userProductMapper.markCancelledIfPendingByUser(userProductId, userId);
        if (taken == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "该购买记录不属于当前用户，或已付款、已取消");
        }
        int released = productMapper.releaseLockedStock(userProduct.getProductId(), userProduct.getProductNumber());
        if (released == 0) {
            log.error("取消回补冻结库存失败，冻结量与购买数量不一致，userProductId={}，productId={}，number={}",
                    userProductId, userProduct.getProductId(), userProduct.getProductNumber());
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        return getById(userProductId);
    }

    /**
     * 功能：超时关闭单条待付款记录并回补冻结库存。
     *
     * <p>与买家主动取消共用同一套状态抢占条件（仅不校验买家归属），谁先抢到谁生效，
     * 天然互斥：买家在超时任务关闭的同一瞬间点付款，也只可能有一方成功。
     *
     * <p>本方法由定时任务逐条调用，每个调用独立成事务。任务层刻意不用批量 UPDATE：
     * 批量改状态后逐一回补，中途失败会留下"已取消但没回补"的记录，而这类损失是静默的
     * （库存凭空少一件）。逐条处理时单条失败只影响这一条，下一轮扫描还会因为状态仍是 0 而重试。
     *
     * @param userProductId 购买记录 ID
     * @return true 表示本次成功关闭并回补；false 表示记录已不存在、已被付款或已被取消，本轮无需回补
     * @throws BusinessException 冻结量与购买数量对不上（数据已不一致）抛 500 并回滚状态变更
     */
    @Override
    @Transactional
    public boolean closeTimeout(Long userProductId) {
        UserProduct userProduct = userProductMapper.selectById(userProductId);
        if (userProduct == null) {
            return false;
        }
        int taken = userProductMapper.markCancelledIfPending(userProductId);
        if (taken == 0) {
            // 已被付款或已被取消，库存已由那一方处理，本轮不得重复回补
            return false;
        }
        int released = productMapper.releaseLockedStock(userProduct.getProductId(), userProduct.getProductNumber());
        if (released == 0) {
            log.error("超时关闭回补冻结库存失败，冻结量与购买数量不一致，userProductId={}，productId={}，number={}",
                    userProductId, userProduct.getProductId(), userProduct.getProductNumber());
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        return true;
    }

    /**
     * 功能：查询购买记录详情，只允许查看本人记录。
     *
     * <p>购买记录带着买家身份、购买数量与时间，属于交易明细，不能做成"知道 ID 就能看"的
     * 公开读：query 与 delete 早已按 user_id 收口，这里补齐唯一漏掉的一处，消除越权读取。
     *
     * @param userProductId 购买记录 ID
     * @return 购买记录详情
     * @throws BusinessException 记录不存在抛 404，不属于当前登录用户抛 403，登录失效抛 401
     */
    @Override
    public UserProductVO getById(Long userProductId) {
        return toUserProductVO(requireOwnedUserProduct(userProductId));
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

    /**
     * 功能：物理删除当前登录用户自己的购买记录。
     *
     * <p>待付款记录一律拒绝删除：它占着商品的冻结库存，而删除之后这条记录既不会被取消
     * 回补、也不会被超时任务扫到（记录已不存在），冻结量将永远挂在商品上无法收敛。
     * 买家想放弃这笔交易应先调取消接口，让库存归还走正常路径。
     *
     * @param userProductId 购买记录 ID，必须属于当前登录买家且已非待付款
     * @throws BusinessException 记录不存在抛 404，不属于本人抛 403，仍为待付款抛 409
     */
    @Override
    public void delete(Long userProductId) {
        UserProduct userProduct = requireOwnedUserProduct(userProductId);
        if (userProduct.getOrderStatus() != null
                && userProduct.getOrderStatus() == PurchaseStatuses.PENDING_PAYMENT) {
            throw new BusinessException(ErrorCode.CONFLICT, "待付款的购买记录不能直接删除，请先取消");
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

    /**
     * 功能：查询购买记录并校验归属，非本人记录一律拒绝。
     *
     * <p>把"记录存在"与"记录属于我"分成两个方法表达：付款、取消、超时关闭靠条件更新的
     * WHERE user_id 自带归属约束，只需要前者；对外的读接口必须用后者。
     *
     * @param userProductId 购买记录 ID
     * @return 属于当前登录用户的购买记录
     * @throws BusinessException 记录不存在抛 404，不属于当前登录用户抛 403，登录失效抛 401
     */
    private UserProduct requireOwnedUserProduct(Long userProductId) {
        UserProduct userProduct = requireUserProduct(userProductId);
        Long currentUserId = requireCurrentUserId();
        if (!currentUserId.equals(userProduct.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作他人的购买记录");
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
