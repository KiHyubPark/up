package com.clone.up.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@EnableAsync
@Configuration
public class AsyncConfig {

    /**
     * 캔들 수집 전용 단일 스레드 executor
     * — 업비트 레이트 리미터(초당 8회)와 충돌 방지를 위해 동시 수집은 1개로 제한
     */
    @Bean(name = "candleCollectExecutor")
    public Executor candleCollectExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(5);
        executor.setThreadNamePrefix("candle-collect-");
        executor.initialize();
        return executor;
    }
}
