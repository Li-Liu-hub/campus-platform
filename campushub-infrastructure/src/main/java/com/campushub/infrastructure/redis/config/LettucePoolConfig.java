package com.campushub.infrastructure.redis;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.util.StringUtils;

import io.lettuce.core.api.StatefulConnection;

/**
 * 功能：接管 LettuceConnectionFactory 的创建，支持用配置开关切换"共享单连接"与"连接池"两种模式。
 *
 * <p>背景：Lettuce 默认 shareNativeConnection=true，所有常规读写复用一条原生连接，
 * 即使 Boot 依据 spring.data.redis.lettuce.pool.* 建了池，普通命令也不走池；
 * 且该属性未暴露为配置项，只能在创建工厂时显式设置。本配置类定义的 Bean 会使
 * Boot 的 RedisAutoConfiguration 自动让位（@ConditionalOnMissingBean）。
 *
 * <p>传入参数：campushub.redis.pool-enabled——false 时行为与 Boot 默认一致
 * （共享单连接、无池），true 时按 yml 池参数建池并关闭共享连接。
 *
 * <p>返回参数：LettuceConnectionFactory，供 RedisTemplate 等组件使用。
 */
@Configuration
public class LettucePoolConfig {

    /** 池化开关：true 普通读写从池中借还连接，false 维持 Boot 默认共享单连接 */
    @Value("${campushub.redis.pool-enabled:false}")
    private boolean poolEnabled;

    @Bean
    public LettuceConnectionFactory redisConnectionFactory(RedisProperties properties) {
        LettuceClientConfiguration clientConfig;
        if (poolEnabled) {
            clientConfig = LettucePoolingClientConfiguration.builder()
                    .poolConfig(buildPoolConfig(properties))
                    .commandTimeout(properties.getTimeout())
                    .build();
        } else {
            clientConfig = LettuceClientConfiguration.builder()
                    .commandTimeout(properties.getTimeout())
                    .build();
        }
        LettuceConnectionFactory factory =
                new LettuceConnectionFactory(standaloneConfig(properties), clientConfig);
        // 关键：池化 arm 关闭共享连接，普通读写才真正从池中借还
        factory.setShareNativeConnection(!poolEnabled);
        return factory;
    }

    /** 将 yml 的 host/port/账号/库号映射为独立连接配置（本项目仅使用单机 Redis） */
    private RedisStandaloneConfiguration standaloneConfig(RedisProperties properties) {
        RedisStandaloneConfiguration config =
                new RedisStandaloneConfiguration(properties.getHost(), properties.getPort());
        config.setDatabase(properties.getDatabase());
        if (StringUtils.hasText(properties.getUsername())) {
            config.setUsername(properties.getUsername());
        }
        String password = properties.getPassword();
        if (StringUtils.hasText(password)) {
            config.setPassword(RedisPassword.of(password));
        }
        return config;
    }

    /** 从 yml 读取池参数构建 commons-pool2 配置 */
    private GenericObjectPoolConfig<StatefulConnection<?, ?>> buildPoolConfig(RedisProperties properties) {
        RedisProperties.Pool pool = properties.getLettuce().getPool();
        GenericObjectPoolConfig<StatefulConnection<?, ?>> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(pool.getMaxActive());
        config.setMaxIdle(pool.getMaxIdle());
        config.setMinIdle(pool.getMinIdle());
        if (pool.getMaxWait() != null) {
            config.setMaxWait(pool.getMaxWait());
        }
        if (pool.getTimeBetweenEvictionRuns() != null) {
            config.setTimeBetweenEvictionRuns(pool.getTimeBetweenEvictionRuns());
        }
        return config;
    }
}
