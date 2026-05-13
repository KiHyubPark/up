package com.clone.up.domain.trading.entity;

import com.clone.up.domain.candle.entity.CandleType;
import com.clone.up.domain.strategy.StrategyType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 자동매매 포지션 영속화 엔티티.
 *
 * <p>서버 재시작 후에도 포지션 상태를 복구할 수 있도록 DB에 저장한다.
 * 동일 (market, strategyType) 조합의 OPEN 포지션은 서비스 레이어에서 1개로 제한한다.
 *
 * <p>entryAtrValue: 진입 시점 ATR값 — 재시작 후 ATR 손절가 재계산에 사용된다.
 */
@Entity
@Table(name = "live_position")
public class LivePosition {

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
    @Column(nullable = false, length = 10)
    private PositionStatus status;

    @Column(nullable = false, precision = 30, scale = 8)
    private BigDecimal entryPrice;

    @Column(nullable = false)
    private LocalDateTime entryTime;

    /** 진입 시점 ATR값 — atrStopMultiplier × entryAtrValue = 손절 하락폭 */
    @Column(nullable = false, precision = 30, scale = 8)
    private BigDecimal entryAtrValue;

    @Column(nullable = false, precision = 30, scale = 8)
    private BigDecimal quantity;

    /** entryPrice × quantity */
    @Column(nullable = false, precision = 30, scale = 8)
    private BigDecimal investedAmount;

    // 청산 시 채워지는 필드 (OPEN 상태에서는 null)
    @Column(precision = 30, scale = 8)
    private BigDecimal exitPrice;

    private LocalDateTime exitTime;

    /** (exitPrice - entryPrice) × quantity — 수수료 미반영 */
    @Column(precision = 30, scale = 8)
    private BigDecimal realizedPnl;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected LivePosition() {
    }

    public static LivePosition open(
            String market,
            StrategyType strategyType,
            CandleType candleType,
            BigDecimal entryPrice,
            LocalDateTime entryTime,
            BigDecimal entryAtrValue,
            BigDecimal quantity) {
        LivePosition p = new LivePosition();
        p.market = market;
        p.strategyType = strategyType;
        p.candleType = candleType;
        p.status = PositionStatus.OPEN;
        p.entryPrice = entryPrice;
        p.entryTime = entryTime;
        p.entryAtrValue = entryAtrValue;
        p.quantity = quantity;
        p.investedAmount = entryPrice.multiply(quantity);
        p.createdAt = LocalDateTime.now();
        p.updatedAt = LocalDateTime.now();
        return p;
    }

    /**
     * 포지션을 청산 상태로 전환한다.
     * 수익 = (exitPrice - entryPrice) × quantity
     */
    public void close(BigDecimal exitPrice, LocalDateTime exitTime) {
        this.exitPrice = exitPrice;
        this.exitTime = exitTime;
        this.realizedPnl = exitPrice.subtract(entryPrice).multiply(quantity);
        this.status = PositionStatus.CLOSED;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getMarket() { return market; }
    public StrategyType getStrategyType() { return strategyType; }
    public CandleType getCandleType() { return candleType; }
    public PositionStatus getStatus() { return status; }
    public BigDecimal getEntryPrice() { return entryPrice; }
    public LocalDateTime getEntryTime() { return entryTime; }
    public BigDecimal getEntryAtrValue() { return entryAtrValue; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getInvestedAmount() { return investedAmount; }
    public BigDecimal getExitPrice() { return exitPrice; }
    public LocalDateTime getExitTime() { return exitTime; }
    public BigDecimal getRealizedPnl() { return realizedPnl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
