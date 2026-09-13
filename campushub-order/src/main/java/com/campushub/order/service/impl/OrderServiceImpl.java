package com.campushub.order.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campushub.common.exception.BusinessException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.common.log.OperationLog;
import com.campushub.common.log.OperationTypes;
import com.campushub.infrastructure.redis.DistributedLockService;
import com.campushub.infrastructure.security.AuthenticationService;
import com.campushub.order.constant.OrderCategories;
import com.campushub.order.constant.OrderStatuses;
import com.campushub.order.dto.OrderCreateRequest;
import com.campushub.order.dto.OrderQueryRequest;
import com.campushub.order.dto.OrderUpdateRequest;
import com.campushub.order.entity.Order;
import com.campushub.order.entity.OrderImage;
import com.campushub.order.mapper.OrderImageMapper;
import com.campushub.order.mapper.OrderMapper;
import com.campushub.order.service.OrderService;
import com.campushub.order.vo.OrderPageVO;
import com.campushub.order.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** 订单基础业务实现：增删改查 + 分布式锁抢单。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;

    private final OrderImageMapper orderImageMapper;

    private final AuthenticationService authenticationService;

    private final DistributedLockService distributedLockService;

    /** 分页默认页大小。 */
    private static final int DEFAULT_PAGE_SIZE = 20;

    /** 分页最大页大小。 */
    private static final int MAX_PAGE_SIZE = 100;

    /** 抢单锁键前缀，完整键为 campushub:order:lock:{orderId}。 */
    private static final String GRAB_LOCK_KEY_PREFIX = "campushub:order:lock:";

    /** 抢单锁自动释放时长：持锁业务超时未释放时，由 Redis 字段级 TTL 兜底解锁，避免死锁。 */
    private static final Duration GRAB_LOCK_TTL = Duration.ofSeconds(10);

    /** 直接插入创建当前登录用户发布的订单，幂等由数据库唯一键保证；重复提交时返回已存在的原订单。 */
    @Override
    @Transactional
    public OrderVO create(OrderCreateRequest request) {
        Long userId = requireCurrentUserId();
        String idempotencyKey = request.orderIdempotencyKey().trim();
        Order order = new Order();
        order.setOrderSentUserId(userId);
        order.setOrderType(normalizeOrderType(request.orderType()));
        order.setOrderAmount(request.orderAmount());
        order.setOrderTimeout(request.orderTimeout());
        order.setOrderIdempotencyKey(idempotencyKey);
        order.setOrderStatus(OrderStatuses.PENDING);
        order.setOrderViewNumber(0L);
        order.setOrderIsDelete(0);
        try {
            orderMapper.insert(order);
            // 订单唯一键先落位、图片后插入：重复提交撞键时不会插图片，订单幂等是图片幂等的前置闸门
            insertImages(order.getOrderId(), request.imageUrls());
        } catch (DuplicateKeyException exception) {
            // 唯一键冲突说明同一幂等键已插入成功，直接返回已存在的订单；查不到说明命中的是已软删除的订单
            Order existingOrder = orderMapper.selectOne(Wrappers.<Order>lambdaQuery()
                    .eq(Order::getOrderSentUserId, userId)
                    .eq(Order::getOrderIdempotencyKey, idempotencyKey)
                    .last("LIMIT 1"));
            if (existingOrder == null) {
                throw new BusinessException(ErrorCode.CONFLICT, "订单幂等键已被使用");
            }
            return toOrderVO(existingOrder, loadImageUrls(existingOrder.getOrderId()));
        }
        return getById(order.getOrderId());
    }

    /** 查询未删除订单，订单不存在时返回 404 业务异常。 */
    @Override
    public OrderVO getById(Long orderId) {
        Order order = requireOrder(orderId);
        return toOrderVO(order, loadImageUrls(orderId));
    }

    /**
     * 功能：浏览订单，返回详情并将浏览量原子加一。
     *
     * <p>先校验订单存在再记账，浏览量自增后重读返回最新值；单条 UPDATE 原子自增
     * 保证并发下不丢计数，重读允许微小滞后。
     *
     * @param orderId 订单主键，不允许为空
     * @return 订单详情
     * @throws BusinessException 订单 ID 为空时抛 400，订单不存在时抛 404
     */
    @Override
    public OrderVO view(Long orderId) {
        getById(orderId);
        orderMapper.increaseViewNumber(orderId);
        return getById(orderId);
    }

    /** 按角色与状态查询当前登录用户的订单，返回本页数据，按创建时间倒序。 */
    @Override
    public OrderPageVO query(OrderQueryRequest request) {
        Long userId = requireCurrentUserId();
        String role = stringOrDefault(request.role(), "sent");
        int pageSize = resolvePageSize(request.pageSize());
        List<Order> orders = orderMapper.selectByCondition(role, userId, request.orderStatus(), pageSize);
        // 图片一次批量装载，避免逐条回查（N+1）
        Map<Long, List<String>> imageMap = loadImageUrls(orders);
        List<OrderVO> list = orders.stream()
                .map(order -> toOrderVO(order, imageMap.getOrDefault(order.getOrderId(), List.of())))
                .toList();
        return new OrderPageVO(list);
    }

    /** 修改当前登录用户发布的待接单订单；条件更新兜底，订单流转后不可再修改。操作留痕由日志切面异步完成。 */
    @OperationLog(type = OperationTypes.ORDER_UPDATE, targetId = "#args[0]")
    @Override
    public OrderVO update(Long orderId, OrderUpdateRequest request) {
        Long userId = requireCurrentUserId();
        Order changes = new Order();
        if (request.orderType() != null) {
            changes.setOrderType(normalizeOrderType(request.orderType()));
        }
        changes.setOrderAmount(request.orderAmount());
        changes.setOrderTimeout(request.orderTimeout());
        // 条件更新把"属于我 + 仍待接单"作为乐观条件，避免先查后改的并发窗口
        int updated = orderMapper.update(changes, Wrappers.<Order>lambdaUpdate()
                .eq(Order::getOrderId, orderId)
                .eq(Order::getOrderSentUserId, userId)
                .eq(Order::getOrderStatus, OrderStatuses.PENDING));
        if (updated == 0) {
            resolveOwnershipConflict(orderId, userId, "仅待接单状态的订单可以修改");
        }
        return getById(orderId);
    }

    /** 软删除当前登录用户发布的待接单订单，已接单订单需先走取消流程不可直接删除。 */
    @OperationLog(type = OperationTypes.ORDER_DELETE, targetId = "#args[0]")
    @Override
    public void delete(Long orderId) {
        Long userId = requireCurrentUserId();
        // @TableLogic 使 delete 生成为 UPDATE order_is_delete = 1，条件更新兜底与修改一致
        int deleted = orderMapper.delete(Wrappers.<Order>lambdaQuery()
                .eq(Order::getOrderId, orderId)
                .eq(Order::getOrderSentUserId, userId)
                .eq(Order::getOrderStatus, OrderStatuses.PENDING));
        if (deleted == 0) {
            resolveOwnershipConflict(orderId, userId, "仅待接单状态的订单可以删除");
        }
    }

    /**
     * 功能：抢单，分布式锁串行化同一订单的竞争请求，数据库状态条件更新兜底。
     *
     * <p>流程：获取锁（SET NX EX，键级 TTL 10 秒超时自动释放）→ 锁内校验
     * 订单存在、非本人订单、仍待接单 → 条件更新回填接单人（兜底：即使锁失效，
     * 数据库行锁 + 状态条件也保证仅一人成功）→ 释放锁（Lua 脚本比对 token 匹配才删除）。
     *
     * <p>Redis 不可用时降级为直接走数据库条件更新，正确性不受影响，仅竞争烈度变大。
     *
     * @param orderId 订单主键，不允许为空
     * @return 接单成功后的订单详情
     * @throws BusinessException 订单不存在抛 404；抢自己的订单抛 400；
     *         锁被占用、订单已流转或条件更新未命中抛 409
     */
    @OperationLog(type = OperationTypes.ORDER_GRAB, targetId = "#args[0]")
    @Override
    public OrderVO grab(Long orderId) {
        Long userId = requireCurrentUserId();
        String token = UUID.randomUUID().toString();
        String lockKey = GRAB_LOCK_KEY_PREFIX + orderId;
        boolean locked;
        try {
            locked = distributedLockService.tryLock(lockKey, token, GRAB_LOCK_TTL);
        } catch (Exception exception) {
            // Redis 故障降级：跳过锁直接依赖数据库条件更新兜底，正确性不受影响
            log.warn("抢单锁获取失败，降级为数据库条件更新兜底，orderId={}", orderId, exception);
            locked = true;
        }
        if (!locked) {
            throw new BusinessException(ErrorCode.CONFLICT, "订单正在被他人处理，请稍后重试");
        }
        try {
            Order order = requireOrder(orderId);
            if (userId.equals(order.getOrderSentUserId())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "不能抢自己发布的订单");
            }
            if (order.getOrderStatus() != OrderStatuses.PENDING) {
                throw new BusinessException(ErrorCode.CONFLICT, "订单已被接单或已关闭");
            }
            // 状态更新兜底：WHERE order_status = 0 的条件更新是并发正确性的最终保障
            int updated = orderMapper.casGrab(orderId, userId);
            if (updated == 0) {
                throw new BusinessException(ErrorCode.CONFLICT, "手慢了，订单已被他人抢走");
            }
            return getById(orderId);
        } finally {
            try {
                distributedLockService.unlock(lockKey, token);
            } catch (Exception exception) {
                // 释放失败仅记录，锁由键级 TTL 到期自动释放
                log.warn("抢单锁释放失败，等待 TTL 自动过期，orderId={}", orderId, exception);
            }
        }
    }

    /** 根据订单 ID 查询未删除订单，订单不存在时抛出 404 业务异常。 */
    private Order requireOrder(Long orderId) {
        if (orderId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "订单 ID 不能为空");
        }
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        return order;
    }

    /**
     * 功能：条件更新未命中时区分具体原因并抛出对应业务异常。
     *
     * @param orderId 订单主键
     * @param userId 当前登录用户 ID
     * @param statusConflictMessage 状态不满足时的提示语
     * @throws BusinessException 订单不存在抛 404，非本人订单抛 403，状态不满足抛 409
     */
    private void resolveOwnershipConflict(Long orderId, Long userId, String statusConflictMessage) {
        Order order = requireOrder(orderId);
        if (!userId.equals(order.getOrderSentUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作该订单");
        }
        throw new BusinessException(ErrorCode.CONFLICT, statusConflictMessage);
    }

    /** 获取当前登录用户 ID，登录状态失效时抛出 401 业务异常。 */
    private Long requireCurrentUserId() {
        Long userId = authenticationService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录状态已失效");
        }
        return userId;
    }

    /** 校验订单类型在支持列表内并原样返回。 */
    private String normalizeOrderType(String orderType) {
        String normalized = orderType == null ? null : orderType.trim();
        if (normalized == null || !OrderCategories.VALUES.contains(normalized)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "订单类型不支持");
        }
        return normalized;
    }

    /** 页大小缺省 20，并限制在 1 到 100 之间。 */
    private int resolvePageSize(Integer pageSize) {
        if (pageSize == null) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
    }

    /** 字符串参数缺省值处理，空白视为未传。 */
    private String stringOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    /** 逐张插入订单图片，sort 按提交顺序从 0 递增。 */
    private void insertImages(Long orderId, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }
        int sort = 0;
        for (String imageUrl : imageUrls) {
            OrderImage image = new OrderImage();
            image.setOrderImageOrderId(orderId);
            image.setOrderImageUrl(imageUrl);
            image.setOrderImageSort(sort++);
            orderImageMapper.insert(image);
        }
    }

    /** 查询单条订单的图片地址，按展示顺序升序。 */
    private List<String> loadImageUrls(Long orderId) {
        return orderImageMapper.selectList(Wrappers.<OrderImage>lambdaQuery()
                        .eq(OrderImage::getOrderImageOrderId, orderId)
                        .orderByAsc(OrderImage::getOrderImageSort))
                .stream()
                .map(OrderImage::getOrderImageUrl)
                .toList();
    }

    /** 批量查询订单图片地址，一次 IN 查询分组映射，避免逐条回查。 */
    private Map<Long, List<String>> loadImageUrls(List<Order> orders) {
        if (orders.isEmpty()) {
            return Map.of();
        }
        List<Long> orderIds = orders.stream().map(Order::getOrderId).toList();
        List<OrderImage> images = orderImageMapper.selectList(Wrappers.<OrderImage>lambdaQuery()
                .in(OrderImage::getOrderImageOrderId, orderIds)
                .orderByAsc(OrderImage::getOrderImageSort));
        return images.stream().collect(Collectors.groupingBy(OrderImage::getOrderImageOrderId,
                Collectors.mapping(OrderImage::getOrderImageUrl, Collectors.toList())));
    }

    /** 将订单实体转换为接口返回对象，图片单独装载。 */
    private OrderVO toOrderVO(Order order, List<String> imageUrls) {
        return new OrderVO(
                order.getOrderId(),
                order.getOrderSentUserId(),
                order.getOrderReceiveUserId(),
                order.getOrderType(),
                order.getOrderAmount(),
                order.getOrderStatus(),
                order.getOrderTimeout(),
                imageUrls,
                order.getOrderViewNumber(),
                order.getCreateTime(),
                order.getUpdateTime()
        );
    }
}
