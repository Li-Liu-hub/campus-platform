package com.campushub.shop.task;

import com.campushub.shop.config.ShopPurchaseProperties;
import com.campushub.shop.entity.UserProduct;
import com.campushub.shop.mapper.UserProductMapper;
import com.campushub.shop.service.UserProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 功能：超时未付款购买记录的关闭任务，扫描到期的待付款记录，逐条关闭并把冻结库存回补到可卖量。
 *
 * <p>没有这个任务，买家下单不付款就会把库存永久占住——冻结量不会自己归还，
 * 商品会一直显示"交易中"，真实的买家永远等不到货放出来。
 *
 * <p>三条刻意的设计取舍：
 * <ol>
 *   <li><b>逐条处理，不用批量 UPDATE。</b>每条都要"改状态 + 回补库存"两步且各自成事务。
 *       若先批量改状态再逐一回补，中途失败会留下已取消但没回补的记录，而这类损失是
 *       静默的——库存凭空少一件，没人会收到报错。</li>
 *   <li><b>单条失败只跳过，不停摆。</b>失败的那条状态仍是待付款，下一轮扫描会自然重试；
 *       若因一条异常终止整轮，后面的记录会被无谓地推迟。</li>
 *   <li><b>事务边界在 Service 而不是这里。</b>逐条调用 {@code closeTimeout}，每次调用都是一个
 *       独立事务（跨 Bean 调用走代理，注解生效）；任务层本身不开事务，也不能开。</li>
 * </ol>
 *
 * <p>扫描间隔用注解占位符读配置，是因为 {@code @Scheduled} 只接受常量表达式；
 * 超时时长与单批条数被任务与接口层共用，因此集中在 {@link ShopPurchaseProperties} 中。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PurchaseTimeoutCloseTask {

    private final UserProductMapper userProductMapper;

    private final UserProductService userProductService;

    private final ShopPurchaseProperties shopPurchaseProperties;

    /**
     * 功能：按固定间隔扫描并关闭超时未付款的购买记录，间隔可经
     * campushub.shop.timeout-scan-interval-ms 配置，默认 60 秒。
     */
    @Scheduled(fixedDelayString = "${campushub.shop.timeout-scan-interval-ms:60000}")
    public void closeTimeoutPurchases() {
        try {
            LocalDateTime deadline = LocalDateTime.now()
                    .minusMinutes(shopPurchaseProperties.getOrderTimeoutMinutes());
            List<UserProduct> candidates = userProductMapper.selectTimeoutCandidates(
                    deadline, shopPurchaseProperties.getTimeoutBatchSize());
            if (candidates.isEmpty()) {
                return;
            }
            int closed = 0;
            for (UserProduct candidate : candidates) {
                try {
                    if (userProductService.closeTimeout(candidate.getUserProductId())) {
                        closed++;
                    }
                } catch (Exception exception) {
                    log.error("超时关闭单条购买记录失败，跳过继续处理剩余记录，userProductId={}",
                            candidate.getUserProductId(), exception);
                }
            }
            log.info("超时购买记录关闭完成，本批扫描 {} 条，成功关闭 {} 条", candidates.size(), closed);
        } catch (Exception exception) {
            log.error("超时购买记录关闭任务异常，本轮终止，未关闭的记录留待下轮", exception);
        }
    }
}
