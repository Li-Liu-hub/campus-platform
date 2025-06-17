package com.campushub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** CampusHub 启动类，组件扫描覆盖 com.campushub 根包下所有业务模块。 */
@SpringBootApplication
public class CampusHubApplication {

    /** 启动 Spring Boot 应用。 */
    public static void main(String[] args) {
        SpringApplication.run(CampusHubApplication.class, args);
    }
}
