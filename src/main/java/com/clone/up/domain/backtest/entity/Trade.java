package com.clone.up.domain.backtest.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "backtest_trade")
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "backtest_result_id")
    private BacktestResult backtestResult;

    @Enumerated(EnumType.STRING)
    private TradeType tradeType;

    private LocalDateTime tradeTime;
    private BigDecimal price;
    private BigDecimal quantity;
    private BigDecimal commission;

    protected Trade() {
    }

    public static Trade of(
            BacktestResult backtestResult,
            TradeType tradeType,
            LocalDateTime tradeTime,
            BigDecimal price,
            BigDecimal quantity) {
        Trade trade = new Trade();
        trade.backtestResult = backtestResult;
        trade.tradeType = tradeType;
        trade.tradeTime = tradeTime;
        trade.price = price;
        trade.quantity = quantity;
        // 수수료: 0.05%
        trade.commission = price.multiply(quantity).multiply(new BigDecimal("0.0005"));
        return trade;
    }

    public Long getId() { return id; }
    public BacktestResult getBacktestResult() { return backtestResult; }
    public TradeType getTradeType() { return tradeType; }
    public LocalDateTime getTradeTime() { return tradeTime; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getCommission() { return commission; }
}
