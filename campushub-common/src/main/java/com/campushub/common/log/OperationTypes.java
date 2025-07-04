package com.campushub.common.log;

/** 操作日志类型常量，对应 ch_log.log_type 的取值。 */
public final class OperationTypes {

    /** 更新帖子。 */
    public static final String POST_UPDATE = "POST_UPDATE";

    /** 更新订单。 */
    public static final String ORDER_UPDATE = "ORDER_UPDATE";

    /** 删除订单。 */
    public static final String ORDER_DELETE = "ORDER_DELETE";

    /** 抢单。 */
    public static final String ORDER_GRAB = "ORDER_GRAB";

    /** 私有构造，防止实例化常量类。 */
    private OperationTypes() {
    }
}
