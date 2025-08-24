package com.campushub.shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campushub.common.exception.BusinessException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.infrastructure.security.AuthenticationService;
import com.campushub.shop.dto.ShopCreateRequest;
import com.campushub.shop.dto.ShopQueryRequest;
import com.campushub.shop.dto.ShopUpdateRequest;
import com.campushub.shop.entity.Shop;
import com.campushub.shop.mapper.ShopMapper;
import com.campushub.shop.service.ShopService;
import com.campushub.shop.vo.PageVO;
import com.campushub.shop.vo.ShopVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/** 店铺基础业务实现。 */
@Service
@RequiredArgsConstructor
public class ShopServiceImpl implements ShopService {

    private final ShopMapper shopMapper;

    private final AuthenticationService authenticationService;

    /** 游标分页默认页大小。 */
    private static final int DEFAULT_PAGE_SIZE = 20;

    /** 游标分页最大页大小。 */
    private static final int MAX_PAGE_SIZE = 100;

    /** 插入创建当前登录用户的店铺，幂等由数据库唯一键保证；重复提交时返回已存在的原店铺。 */
    @Override
    public ShopVO create(ShopCreateRequest request) {
        Long userId = requireCurrentUserId();
        String idempotencyKey = request.shopIdempotencyKey().trim();
        Shop shop = new Shop();
        shop.setShopUserId(userId);
        shop.setShopName(request.shopName().trim());
        shop.setShopDescription(request.shopDescription());
        shop.setShopIdempotencyKey(idempotencyKey);
        shop.setShopIsDelete(0);
        try {
            shopMapper.insert(shop);
        } catch (DuplicateKeyException exception) {
            // 唯一键冲突说明同一幂等键已插入成功，直接返回已存在的店铺；查不到说明命中的是已软删除的店铺
            Shop existingShop = shopMapper.selectOne(Wrappers.<Shop>lambdaQuery()
                    .eq(Shop::getShopUserId, userId)
                    .eq(Shop::getShopIdempotencyKey, idempotencyKey)
                    .last("LIMIT 1"));
            if (existingShop == null) {
                throw new BusinessException(ErrorCode.CONFLICT, "店铺幂等键已被使用");
            }
            return toShopVO(existingShop);
        }
        return getById(shop.getShopId());
    }

    /** 根据店铺 ID 查询未删除店铺，店铺不存在时返回 404 业务异常。 */
    @Override
    public ShopVO getById(Long shopId) {
        return toShopVO(requireShop(shopId));
    }

    /** 按条件游标分页查询未删除店铺，按创建时间倒序。 */
    @Override
    public PageVO<ShopVO> query(ShopQueryRequest request) {
        int pageSize = resolvePageSize(request.pageSize());
        LambdaQueryWrapper<Shop> wrapper = Wrappers.<Shop>lambdaQuery()
                .eq(request.shopUserId() != null, Shop::getShopUserId, request.shopUserId());
        applyCursor(wrapper, request.cursorTime(), request.cursorId());
        wrapper.orderByDesc(Shop::getCreateTime).orderByDesc(Shop::getShopId)
                .last("LIMIT " + pageSize);
        List<Shop> shops = shopMapper.selectList(wrapper);
        List<ShopVO> list = shops.stream().map(this::toShopVO).toList();
        if (list.size() < pageSize) {
            return new PageVO<>(list, false, null, null);
        }
        // 取满一页说明可能还有更多数据，最后一行即下一页游标
        Shop lastShop = shops.get(shops.size() - 1);
        return new PageVO<>(list, true, lastShop.getCreateTime(), lastShop.getShopId());
    }

    /** 修改当前登录用户的店铺。 */
    @Override
    public ShopVO update(Long shopId, ShopUpdateRequest request) {
        Shop shop = requireOwnedShop(shopId);
        shop.setShopName(request.shopName().trim());
        shop.setShopDescription(request.shopDescription());
        shopMapper.updateById(shop);
        return getById(shopId);
    }

    /** 软删除当前登录用户的店铺。 */
    @Override
    public void delete(Long shopId) {
        Shop shop = requireOwnedShop(shopId);
        // @TableLogic 使 deleteById 生成为 UPDATE shop_is_delete = 1
        shopMapper.deleteById(shop.getShopId());
    }

    /** 根据店铺 ID 查询未删除店铺，店铺不存在时抛出 404 业务异常。 */
    private Shop requireShop(Long shopId) {
        if (shopId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "店铺 ID 不能为空");
        }
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "店铺不存在");
        }
        return shop;
    }

    /** 校验店铺存在且属于当前登录用户，否则抛出对应业务异常。 */
    private Shop requireOwnedShop(Long shopId) {
        Shop shop = requireShop(shopId);
        Long currentUserId = requireCurrentUserId();
        if (!currentUserId.equals(shop.getShopUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能操作自己的店铺");
        }
        return shop;
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

    /** 将店铺实体转换为接口返回对象。 */
    private ShopVO toShopVO(Shop shop) {
        return new ShopVO(
                shop.getShopId(),
                shop.getShopUserId(),
                shop.getShopName(),
                shop.getShopDescription(),
                shop.getShopIdempotencyKey(),
                shop.getCreateTime()
        );
    }

    /** 追加游标条件：按 (create_time, shop_id) 倒序定位上一页末行之后。 */
    private void applyCursor(LambdaQueryWrapper<Shop> wrapper, LocalDateTime cursorTime, Long cursorId) {
        if (cursorTime == null || cursorId == null) {
            return;
        }
        wrapper.and(w -> w.lt(Shop::getCreateTime, cursorTime)
                .or(o -> o.eq(Shop::getCreateTime, cursorTime).lt(Shop::getShopId, cursorId)));
    }
}
