package com.clone.up.domain.trading.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 긴급 중단 스위치.
 *
 * <p>REST API로 활성화하면 스케줄러가 더 이상 매매 로직을 실행하지 않는다.
 * JVM 내 {@link AtomicBoolean} 플래그로 구현하므로 서버 재시작 시 초기화된다.
 *
 * <p>운영 중 이상 감지 시 즉시 활성화하고, 원인 분석 후 비활성화한다.
 */
@Service
public class EmergencyStopService {

    private static final Logger log = LoggerFactory.getLogger(EmergencyStopService.class);

    private final AtomicBoolean stopped = new AtomicBoolean(false);

    /** @return true이면 긴급 중단 활성 상태 */
    public boolean isStopped() {
        return stopped.get();
    }

    /**
     * 긴급 중단을 활성화한다.
     * 이미 활성화된 상태라면 경고 로그만 남기고 무시한다.
     */
    public void activate(String reason) {
        boolean changed = stopped.compareAndSet(false, true);
        if (changed) {
            log.warn("긴급 중단 활성화 — reason={}", reason);
        } else {
            log.warn("긴급 중단 이미 활성 상태 — reason={}", reason);
        }
    }

    /**
     * 긴급 중단을 해제한다.
     * 해제 전에 반드시 원인을 분석하고 확인한 후 호출해야 한다.
     */
    public void deactivate() {
        boolean changed = stopped.compareAndSet(true, false);
        if (changed) {
            log.info("긴급 중단 해제 — 매매 스케줄러 재개");
        } else {
            log.info("긴급 중단 이미 비활성 상태");
        }
    }
}
