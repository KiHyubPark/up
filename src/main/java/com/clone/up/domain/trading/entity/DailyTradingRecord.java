package com.clone.up.domain.trading.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 날짜별 거래 손익 누계.
 *
 * <p>DailyRiskGuard가 이 테이블을 기준으로 일일 손실 한도를 판단한다.
 */
@Entity
@Table(
        name = "daily_trading_record",
        uniqueConstraints = @UniqueConstraint(name = "uk_daily_trading_date", columnNames = "trade_date")
)
public class DailyTradingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    /** 오늘 청산된 포지션들의 realizedPnl 합계 */
    @Column(nullable = false, precision = 30, scale = 8)
    private BigDecimal totalPnl;

    @Column(nullable = false)
    private int tradeCount;

    /**
     * 일일 손실 한도 초과로 거래가 중단된 상태.
     * true이면 당일 더 이상 진입하지 않는다.
     */
    @Column(nullable = false)
    private boolean halted;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected DailyTradingRecord() {
    }

    public static DailyTradingRecord startOfDay(LocalDate date) {
        DailyTradingRecord record = new DailyTradingRecord();
        record.tradeDate = date;
        record.totalPnl = BigDecimal.ZERO;
        record.tradeCount = 0;
        record.halted = false;
        record.updatedAt = LocalDateTime.now();
        return record;
    }

    /** 청산 발생 시 손익을 누계하고, 한도 초과 여부를 판단한다 */
    public void recordClose(BigDecimal pnl, BigDecimal initialCapital, double dailyLossLimitPercent) {
        this.totalPnl = this.totalPnl.add(pnl);
        this.tradeCount++;
        this.updatedAt = LocalDateTime.now();

        // 손실이 일일 한도를 넘으면 당일 거래 중단
        double lossThreshold = initialCapital.doubleValue() * dailyLossLimitPercent / 100.0;
        if (this.totalPnl.doubleValue() < -lossThreshold) {
            this.halted = true;
        }
    }

    public Long getId() { return id; }
    public LocalDate getTradeDate() { return tradeDate; }
    public BigDecimal getTotalPnl() { return totalPnl; }
    public int getTradeCount() { return tradeCount; }
    public boolean isHalted() { return halted; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
