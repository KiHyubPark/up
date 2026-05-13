package com.clone.up.domain.trading.scheduler;

import com.clone.up.config.TradingProperties;
import com.clone.up.domain.trading.service.TradingExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 자동매매 스케줄러.
 *
 * <p>15분 캔들 마감 1분 후(XX:01, XX:16, XX:31, XX:46)에 실행하여
 * 업비트 데이터 전파 지연을 흡수한다.
 *
 * <p>{@code trading.scheduler-enabled=true}일 때만 실제 실행된다.
 * 로컬·테스트 환경에서는 false로 설정해 스케줄러를 비활성화할 수 있다.
 */
@Component
public class TradingScheduler {

    private static final Logger log = LoggerFactory.getLogger(TradingScheduler.class);

    private final TradingExecutionService executionService;
    private final TradingProperties properties;

    public TradingScheduler(TradingExecutionService executionService, TradingProperties properties) {
        this.executionService = executionService;
        this.properties = properties;
    }

    /**
     * 15분 캔들 마감 후 1분 뒤에 실행 (데이터 전파 여유).
     *
     * <p>cron: 초(0) 분(1,16,31,46) 시(*) 일(*) 월(*) 요일(*)
     */
    @Scheduled(cron = "0 1,16,31,46 * * * *")
    public void runTradingCycle() {
        if (!properties.isSchedulerEnabled()) {
            return;
        }

        log.info("매매 사이클 시작 — market={}, strategy={}, mode={}",
                properties.getMarket(), properties.getStrategyType(), properties.getMode());

        try {
            executionService.execute();
        } catch (Exception e) {
            log.error("매매 사이클 오류 — market={}, error={}",
                    properties.getMarket(), e.getMessage(), e);
        }

        log.info("매매 사이클 완료 — market={}", properties.getMarket());
    }
}
