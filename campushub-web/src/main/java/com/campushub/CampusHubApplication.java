package com.campushub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * CampusHub 启动类，组件扫描覆盖 com.campushub 根包下所有业务模块。
 *
 * <p>启用定时任务调度，支撑发件箱轮询中继、浏览量聚合刷库等后台任务。
 */
@EnableScheduling
@SpringBootApplication
public class CampusHubApplication {

    /** 启动 Spring Boot 应用。 */
    public static void main(String[] args) {
        SpringApplication.run(CampusHubApplication.class, args);
    }
}
