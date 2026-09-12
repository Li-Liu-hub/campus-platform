package com.campushub.shop.constant;

/**
 * 商品展示态：由可卖库存、冻结库存与上架状态三者推导，**不落库**。
 *
 * <p>之所以不存成数据库字段，是因为它表达的是"数量关系的结论"而非独立事实：
 * 一旦落库就成了第二处真相，状态说"交易中"而冻结量恰好为 0 时无法判断谁对。
 *
 * <p>"交易中"是方案里的关键补充：多件商品还有可卖量时展示态仍是"可购买"，
 * 只有可卖量归零而冻结量大于零才显示"交易中"，让买家能区分"卖完了"和
 * "有人在付款中、过一会儿可能放出来"。
 *
 * <p>本枚举以<b>枚举名</b>作为对前端的契约（如 ON_SALE），中文文案由前端自行映射，
 * 后端不返回文案。枚举名稳定、可做翻译键，文案改了不需要动接口。
 */
public enum ProductDisplayState {

    /** 可购买：还有可卖库存，文案可用"剩余 N 件"。 */
    ON_SALE,

    /** 交易中：可卖量为 0 但有买家已下单未付款，文案可用"有人在付款中，可稍后再看"。 */
    IN_TRADE,

    /** 已售罄：可卖量与冻结量都为 0，货已卖完。 */
    SOLD_OUT,

    /** 已下架：卖家主动撤下，与库存数量无关，文案可用"卖家已下架"。 */
    OFF_SHELF;

    /**
     * 功能：由库存与上架状态推导展示态。
     *
     * <p>判断顺序不可调换：卖家已下架的商品必须优先判定为"已下架"，
     * 否则一件被撤下但仍有库存的商品会显示成"可购买"。
     *
     * @param stock 可卖库存，为 null 时按 0 处理
     * @param lockedStock 冻结库存，为 null 时按 0 处理
     * @param status 上架状态：1 上架，0 下架
     * @return 推导出的展示态，已软删除的商品不参与推导（查询层已过滤）
     */
    public static ProductDisplayState derive(Long stock, Long lockedStock, Integer status) {
        if (status != null && status == ProductStatuses.OFF_SHELF) {
            return OFF_SHELF;
        }
        long available = stock == null ? 0L : stock;
        long locked = lockedStock == null ? 0L : lockedStock;
        if (available > 0) {
            return ON_SALE;
        }
        // 可卖量为 0：有冻结量说明有人在付款中，否则才是真正卖完
        return locked > 0 ? IN_TRADE : SOLD_OUT;
    }
}
