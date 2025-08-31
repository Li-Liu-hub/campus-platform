package com.campushub.shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campushub.common.exception.BusinessException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.infrastructure.security.AuthenticationService;
import com.campushub.shop.dto.ProductCreateRequest;
import com.campushub.shop.dto.ProductQueryRequest;
import com.campushub.shop.dto.ProductUpdateRequest;
import com.campushub.shop.entity.Product;
import com.campushub.shop.entity.Shop;
import com.campushub.shop.mapper.ProductMapper;
import com.campushub.shop.mapper.ShopMapper;
import com.campushub.shop.service.ProductService;
import com.campushub.shop.vo.PageVO;
import com.campushub.shop.vo.ProductVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** 商品基础业务实现。 */
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;

    private final ShopMapper shopMapper;

    private final AuthenticationService authenticationService;

    /** 游标分页默认页大小。 */
    private static final int DEFAULT_PAGE_SIZE = 20;

    /** 游标分页最大页大小。 */
    private static final int MAX_PAGE_SIZE = 100;

    /** 商品上架状态。 */
    private static final int STATUS_ON_SHELF = 1;

    /** 在当前登录用户自己的店铺下创建商品，幂等由数据库唯一键保证；重复提交时返回已存在的原商品。 */
    @Override
    public ProductVO create(ProductCreateRequest request) {
        requireOwnedShop(request.shopId());
        String idempotencyKey = request.productIdempotencyKey().trim();
        Product product = new Product();
        product.setProductShopId(request.shopId());
        product.setProductName(request.productName().trim());
        product.setProductPrice(request.productPrice());
        product.setProductStock(request.productStock());
        product.setProductStatus(STATUS_ON_SHELF);
        product.setProductIdempotencyKey(idempotencyKey);
        product.setProductIsDelete(0);
        try {
            productMapper.insert(product);
        } catch (DuplicateKeyException exception) {
            // 唯一键冲突说明同一幂等键已插入成功，直接返回已存在的商品；查不到说明命中的是已软删除的商品
            Product existingProduct = productMapper.selectOne(Wrappers.<Product>lambdaQuery()
                    .eq(Product::getProductShopId, request.shopId())
                    .eq(Product::getProductIdempotencyKey, idempotencyKey)
                    .last("LIMIT 1"));
            if (existingProduct == null) {
                throw new BusinessException(ErrorCode.CONFLICT, "商品幂等键已被使用");
            }
            return toProductVO(existingProduct);
        }
        return getById(product.getProductId());
    }

    /** 根据商品 ID 查询未删除商品，商品不存在时返回 404 业务异常。 */
    @Override
    public ProductVO getById(Long productId) {
        return toProductVO(requireProduct(productId));
    }

    /** 按店铺游标分页查询未删除商品，按创建时间倒序。 */
    @Override
    public PageVO<ProductVO> query(ProductQueryRequest request) {
        int pageSize = resolvePageSize(request.pageSize());
        LambdaQueryWrapper<Product> wrapper = Wrappers.<Product>lambdaQuery()
                .eq(Product::getProductShopId, request.shopId());
        applyCursor(wrapper, request.cursorTime(), request.cursorId());
        wrapper.orderByDesc(Product::getCreateTime).orderByDesc(Product::getProductId)
                .last("LIMIT " + pageSize);
        List<Product> products = productMapper.selectList(wrapper);
        List<ProductVO> list = products.stream().map(this::toProductVO).toList();
        if (list.size() < pageSize) {
            return new PageVO<>(list, false, null, null);
        }
        // 取满一页说明可能还有更多数据，最后一行即下一页游标
        Product lastProduct = products.get(products.size() - 1);
        return new PageVO<>(list, true, lastProduct.getCreateTime(), lastProduct.getProductId());
    }

    /**
     * 功能：修改自己店铺下的商品，商品名称、价格、上架状态按传入值更新，库存只按调整量增减。
     *
     * <p>库存刻意不走字段覆盖：这里查出来的商品实体带着读取时刻的库存快照，若直接整体
     * updateById，就会把快照之后并发下单已经扣掉的库存又写回去，造成可售数量大于实际库存。
     * 所以先构造只含待改字段的实体（库存字段留空，MyBatis-Plus 默认策略不会写入该列），
     * 再用条件更新按调整量增减库存。整段操作在同一事务内，库存调整失败会连带回滚字段修改。
     *
     * @param productId 商品 ID，必须属于当前登录用户的店铺
     * @param request 修改请求，含名称、价格、上架状态与库存调整量（正数补货、负数减库、0 表示不调整）
     * @return 修改后的商品信息
     * @throws BusinessException 商品不存在抛 404，不属于自己抛 403，库存减少量超过当前库存抛 409
     */
    @Override
    @Transactional
    public ProductVO update(Long productId, ProductUpdateRequest request) {
        Product product = requireOwnedProduct(productId);
        // 只带真正要改的字段，避免把读出来的库存快照写回数据库
        Product changed = new Product();
        changed.setProductId(product.getProductId());
        changed.setProductName(request.productName().trim());
        changed.setProductPrice(request.productPrice());
        changed.setProductStatus(request.productStatus());
        productMapper.updateById(changed);
        long stockDelta = request.productStockDelta();
        if (stockDelta != 0 && productMapper.adjustStock(product.getProductId(), stockDelta) == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "库存减少数量超过当前库存");
        }
        return getById(productId);
    }

    /** 软删除自己店铺下的商品。 */
    @Override
    public void delete(Long productId) {
        Product product = requireOwnedProduct(productId);
        // @TableLogic 使 deleteById 生成为 UPDATE product_is_delete = 1
        productMapper.deleteById(product.getProductId());
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

    /** 校验店铺存在且属于当前登录用户，否则抛出对应业务异常。 */
    private Shop requireOwnedShop(Long shopId) {
        if (shopId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "店铺 ID 不能为空");
        }
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "店铺不存在");
        }
        Long currentUserId = requireCurrentUserId();
        if (!currentUserId.equals(shop.getShopUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能在自己的店铺下发布商品");
        }
        return shop;
    }

    /** 校验商品存在且其所属店铺属于当前登录用户，否则抛出对应业务异常。 */
    private Product requireOwnedProduct(Long productId) {
        Product product = requireProduct(productId);
        Shop shop = shopMapper.selectById(product.getProductShopId());
        Long currentUserId = requireCurrentUserId();
        if (shop == null || !currentUserId.equals(shop.getShopUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能操作自己店铺的商品");
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

    /** 将商品实体转换为接口返回对象。 */
    private ProductVO toProductVO(Product product) {
        return new ProductVO(
                product.getProductId(),
                product.getProductShopId(),
                product.getProductName(),
                product.getProductPrice(),
                product.getProductStock(),
                product.getProductStatus(),
                product.getProductIdempotencyKey(),
                product.getCreateTime()
        );
    }

    /** 追加游标条件：按 (create_time, product_id) 倒序定位上一页末行之后。 */
    private void applyCursor(LambdaQueryWrapper<Product> wrapper, LocalDateTime cursorTime, Long cursorId) {
        if (cursorTime == null || cursorId == null) {
            return;
        }
        wrapper.and(w -> w.lt(Product::getCreateTime, cursorTime)
                .or(o -> o.eq(Product::getCreateTime, cursorTime).lt(Product::getProductId, cursorId)));
    }
}
