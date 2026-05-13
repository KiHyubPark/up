package com.clone.up.domain.trading.scheduler;

import com.clone.up.config.TradingProperties;
import com.clone.up.domain.strategy.StrategyFactory;
import com.clone.up.domain.trading.service.TradingExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 자동매매 스케줄러.
 *
 * <p>5분 캔들 마감 30초 후(XX:00:30, XX:05:30, ...)에 실행하여
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
    private final StrategyFactory strategyFactory;

    public TradingScheduler(TradingExecutionService executionService,
                            TradingProperties properties,
                            StrategyFactory strategyFactory) {
        this.executionService = executionService;
        this.properties = properties;
        this.strategyFactory = strategyFactory;
    }

    /**
     * 5분 캔들 마감 후 30초 뒤에 실행 (데이터 전파 여유).
     *
     * <p>cron: 초(30) 분(0/5) 시(*) 일(*) 월(*) 요일(*)
     */
    @Scheduled(cron = "30 0/5 * * * *")
    public void runTradingCycle() {
        if (!properties.isSchedulerEnabled()) {
            return;
        }

        log.info("매매 사이클 시작 — market={}, strategy={}, mode={}",
                properties.getMarket(), strategyFactory.strategyType(), properties.getMode());

        try {
            executionService.execute();
        } catch (Exception e) {
            log.error("매매 사이클 오류 — market={}, error={}",
                    properties.getMarket(), e.getMessage(), e);
        }

        log.info("매매 사이클 완료 — market={}", properties.getMarket());
    }
}
