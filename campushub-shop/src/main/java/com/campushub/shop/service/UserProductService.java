package com.campushub.shop.service;

import com.campushub.shop.dto.UserProductCreateRequest;
import com.campushub.shop.dto.UserProductQueryRequest;
import com.campushub.shop.vo.PageVO;
import com.campushub.shop.vo.UserProductVO;

/** 用户购买商品基础业务接口。 */
public interface UserProductService {

    /** 下单：校验商品在售并冻结库存后创建待付款的购买记录。 */
    UserProductVO create(UserProductCreateRequest request);

    /** 查询本人购买记录详情，非本人记录一律拒绝。 */
    UserProductVO getById(Long userProductId);

    /** 游标分页查询当前登录用户自己的购买记录。 */
    PageVO<UserProductVO> query(UserProductQueryRequest request);

    /**
     * 物理删除当前登录用户自己的购买记录。
     *
     * <p>只允许删除已付款或已取消的记录：待付款记录被删掉后，它占用的冻结库存将永远
     * 没有归还路径（既不会被取消回补，也不会被超时任务扫到，因为记录已经不存在），
     * 所以要求买家先取消再删除。删除动作本身刻意不碰库存，避免"删记录"隐式改动商品状态。
     */
    void delete(Long userProductId);

    /** 买家付款：把待付款记录置为已付款并消耗冻结库存量。 */
    UserProductVO pay(Long userProductId);

    /** 买家主动取消：把待付款记录置为已取消并把冻结库存回补到可卖量。 */
    UserProductVO cancel(Long userProductId);

    /**
     * 超时关闭单条待付款记录并回补冻结库存，供定时任务逐条调用。
     *
     * <p>与买家主动取消共用同一套状态抢占条件，谁先抢到谁生效，天然互斥。
     * 每个调用独立成事务，任务层不需要也不应该再包一层事务。
     *
     * @param userProductId 购买记录 ID
     * @return true 表示本次成功关闭并回补；false 表示记录已不存在、已被付款或已被取消，无需回补
     */
    boolean closeTimeout(Long userProductId);
}
