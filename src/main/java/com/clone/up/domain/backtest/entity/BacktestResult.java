package com.clone.up.domain.backtest.entity;

import com.clone.up.domain.candle.entity.CandleType;
import com.clone.up.domain.strategy.StrategyType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "backtest_result")
public class BacktestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String market;

    @Enumerated(EnumType.STRING)
    private CandleType candleType;

    @Enumerated(EnumType.STRING)
    private StrategyType strategyType;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private BigDecimal initialCapital;
    private BigDecimal finalValue;

    @Embedded
    private PerformanceMetrics metrics;

    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "backtestResult", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Trade> trades = new ArrayList<>();

    protected BacktestResult() {
    }

    public static BacktestResult of(
            String market,
            CandleType candleType,
            StrategyType strategyType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            BigDecimal initialCapital,
            BigDecimal finalValue,
            PerformanceMetrics metrics) {
        BacktestResult result = new BacktestResult();
        result.market = market;
        result.candleType = candleType;
        result.strategyType = strategyType;
        result.startDate = startDate;
        result.endDate = endDate;
        result.initialCapital = initialCapital;
        result.finalValue = finalValue;
        result.metrics = metrics;
        result.createdAt = LocalDateTime.now();
        return result;
    }

    public void addTrade(Trade trade) {
        trades.add(trade);
    }

    public Long getId() { return id; }
    public String getMarket() { return market; }
    public CandleType getCandleType() { return candleType; }
    public StrategyType getStrategyType() { return strategyType; }
    public LocalDateTime getStartDate() { return startDate; }
    public LocalDateTime getEndDate() { return endDate; }
    public BigDecimal getInitialCapital() { return initialCapital; }
    public BigDecimal getFinalValue() { return finalValue; }
    public PerformanceMetrics getMetrics() { return metrics; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<Trade> getTrades() { return List.copyOf(trades); }
}
