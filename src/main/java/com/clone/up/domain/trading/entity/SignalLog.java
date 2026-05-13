package com.clone.up.domain.trading.entity;

import com.clone.up.domain.candle.entity.CandleType;
import com.clone.up.domain.strategy.StrategyType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 전략 시그널 발생 이력.
 *
 * <p>PAPER 모드에서는 실제 포지션 변동 없이 이 테이블에만 기록된다.
 * LIVE 모드에서는 포지션 처리 후 감사 목적으로 함께 기록된다.
 */
@Entity
@Table(name = "signal_log")
public class SignalLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String market;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StrategyType strategyType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CandleType candleType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private SignalType signalType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TradingMode mode;

    /** 시그널 발생 시점의 캔들 종가 */
    @Column(nullable = false, precision = 30, scale = 8)
    private BigDecimal triggerPrice;

    /** 추가 정보 (억제 사유 등) */
    @Column(length = 200)
    private String note;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected SignalLog() {
    }

    public static SignalLog of(
            String market,
            StrategyType strategyType,
            CandleType candleType,
            SignalType signalType,
            TradingMode mode,
            BigDecimal triggerPrice,
            String note) {
        SignalLog log = new SignalLog();
        log.market = market;
        log.strategyType = strategyType;
        log.candleType = candleType;
        log.signalType = signalType;
        log.mode = mode;
        log.triggerPrice = triggerPrice;
        log.note = note;
        log.createdAt = LocalDateTime.now();
        return log;
    }

    public Long getId() { return id; }
    public String getMarket() { return market; }
    public StrategyType getStrategyType() { return strategyType; }
    public CandleType getCandleType() { return candleType; }
    public SignalType getSignalType() { return signalType; }
    public TradingMode getMode() { return mode; }
    public BigDecimal getTriggerPrice() { return triggerPrice; }
    public String getNote() { return note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
