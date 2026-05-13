package com.clone.up.domain.trading.service;

import com.clone.up.domain.strategy.StrategyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 중복 주문 방지 컴포넌트.
 *
 * <p>동일 (market, strategyType) 조합에 대해 동시에 두 쓰레드가 주문을 시도할 때
 * 첫 번째만 통과시키고 나머지는 즉시 예외를 던진다.
 *
 * <p>사용법:
 * <pre>
 *   try (var lock = orderGuard.acquire(market, strategyType)) {
 *       positionService.openPosition(...);
 *   }
 * </pre>
 *
 * <p>주의: 이 락은 단일 JVM 내에서만 유효하다.
 * 다중 인스턴스 환경에서는 DB 비관적 락(LivePositionRepository)이 추가 방어선이다.
 */
@Component
public class OrderGuard {

    private static final Logger log = LoggerFactory.getLogger(OrderGuard.class);

    private final Set<String> processing = ConcurrentHashMap.newKeySet();

    /**
     * 락을 획득한다. try-with-resources로 사용하면 블록 종료 시 자동 해제된다.
     *
     * @throws DuplicateOrderException 이미 처리 중인 주문이 있는 경우
     */
    public OrderLock acquire(String market, StrategyType strategyType) {
        String key = key(market, strategyType);
        if (!processing.add(key)) {
            log.warn("중복 주문 차단 — key={}", key);
            throw new DuplicateOrderException("처리 중인 주문이 있습니다: " + key);
        }
        log.debug("주문 락 획득 — key={}", key);
        return () -> {
            processing.remove(key);
            log.debug("주문 락 해제 — key={}", key);
        };
    }

    private String key(String market, StrategyType strategyType) {
        return market + ":" + strategyType;
    }

    @FunctionalInterface
    public interface OrderLock extends AutoCloseable {
        @Override
        void close(); // 체크 예외 없이 override
    }

    public static class DuplicateOrderException extends RuntimeException {
        public DuplicateOrderException(String message) {
            super(message);
        }
    }
}
