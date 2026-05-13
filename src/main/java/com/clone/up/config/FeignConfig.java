package com.clone.up.config;

import com.google.common.util.concurrent.RateLimiter;
import feign.Logger;
import feign.Request;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(5_000, 10_000); // connectTimeout, readTimeout (ms)
    }

    @Bean
    public RequestInterceptor rateLimitInterceptor() {
        RateLimiter limiter = RateLimiter.create(4.0); // 업비트 공개 API: 초당 10회 한도, 보수적 4회/s
        return template -> limiter.acquire();
    }
}
