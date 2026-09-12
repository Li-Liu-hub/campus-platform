package com.campushub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

/**
 * CampusHub 启动类，组件扫描覆盖 com.campushub 根包下所有业务模块。
 *
 * <p>启用定时任务调度，支撑发件箱轮询中继、浏览量聚合刷库等后台任务。
 */
@EnableScheduling
@SpringBootApplication
public class CampusHubApplication {

    /**
     * 启动 Spring Boot 应用。
     *
     * <p>启动前把 JVM 默认时区固定为东八区：业务时间全部以 LocalDateTime.now() 产生，
     * 取值依赖 JVM 默认时区，而 MySQL 容器默认 UTC。若不固定，本地（宿主东八区）
     * 运行时 JVM 取东八区、数据库取 UTC，两侧时钟相差 8 小时，超时关闭任务按
     * 应用时钟算出 deadline 去比较数据库时钟写入的下单时间会恒成立，
     * 导致刚提交的待付款订单被立即误判超时关闭。固定后两侧一致。
     */
    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
        SpringApplication.run(CampusHubApplication.class, args);
    }
}
