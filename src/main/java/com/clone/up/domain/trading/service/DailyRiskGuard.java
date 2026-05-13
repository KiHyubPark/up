package com.clone.up.domain.trading.service;

import com.clone.up.config.TradingProperties;
import com.clone.up.domain.trading.entity.DailyTradingRecord;
import com.clone.up.domain.trading.repository.DailyTradingRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 일일 손실 한도 관리 서비스.
 *
 * <p>오늘 날짜의 {@link DailyTradingRecord}를 기준으로 거래 가능 여부를 판단하고,
 * 청산 발생 시 PnL을 누계한다.
 *
 * <p>halted=true 이면 당일 더 이상 진입하지 않는다.
 * 자정 이후 새 날이 시작되면 새 레코드를 생성하여 자동 초기화된다.
 */
@Service
public class DailyRiskGuard {

    private static final Logger log = LoggerFactory.getLogger(DailyRiskGuard.class);

    private final DailyTradingRecordRepository repository;
    private final TradingProperties properties;

    public DailyRiskGuard(DailyTradingRecordRepository repository, TradingProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    /**
     * 오늘 거래가 가능한지 확인한다.
     *
     * @return true이면 진입 가능, false이면 일일 한도 초과로 차단
     */
    @Transactional(readOnly = true)
    public boolean isTradeAllowed() {
        return repository.findByTradeDate(LocalDate.now())
                .map(r -> {
                    if (r.isHalted()) {
                        log.warn("일일 손실 한도 초과 — 오늘 거래 차단됨. totalPnl={}", r.getTotalPnl());
                        return false;
                    }
                    return true;
                })
                .orElse(true); // 오늘 레코드 없으면 아직 거래 없음 → 허용
    }

    /**
     * 포지션 청산 시 호출한다.
     *
     * <p>오늘 레코드에 PnL을 누계하고, 한도 초과 여부를 업데이트한다.
     * 레코드가 없으면 신규 생성한다.
     *
     * @param pnl 청산 손익 (음수 = 손실)
     */
    @Transactional
    public void recordClose(BigDecimal pnl) {
        DailyTradingRecord record = repository.findByTradeDate(LocalDate.now())
                .orElseGet(() -> {
                    DailyTradingRecord newRecord = DailyTradingRecord.startOfDay(LocalDate.now());
                    return repository.save(newRecord);
                });

        record.recordClose(pnl, properties.getInitialCapital(), properties.getDailyLossLimitPercent());

        if (record.isHalted()) {
            log.warn("일일 손실 한도 초과 — 당일 거래 중단. totalPnl={}, threshold=-{}%",
                    record.getTotalPnl(), properties.getDailyLossLimitPercent());
        } else {
            log.info("청산 기록 — pnl={}, 오늘 누계={}, 횟수={}",
                    pnl, record.getTotalPnl(), record.getTradeCount());
        }
    }

    /**
     * 오늘 거래 레코드를 조회한다 (없으면 초기값 레코드를 반환).
     */
    @Transactional(readOnly = true)
    public DailyTradingRecord todayRecord() {
        return repository.findByTradeDate(LocalDate.now())
                .orElse(DailyTradingRecord.startOfDay(LocalDate.now()));
    }
}
