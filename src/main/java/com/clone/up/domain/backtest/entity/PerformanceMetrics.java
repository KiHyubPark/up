package com.clone.up.domain.backtest.entity;

import jakarta.persistence.Embeddable;

@Embeddable
public class PerformanceMetrics {

    private double totalReturn;
    private double maxDrawdown;
    private double winRate;
    private double sharpeRatio;
    private double calmarRatio;
    private double profitFactor;
    private int totalTrades;
    private double buyAndHoldReturn;

    protected PerformanceMetrics() {
    }

    public PerformanceMetrics(
            double totalReturn,
            double maxDrawdown,
            double winRate,
            double sharpeRatio,
            double calmarRatio,
            double profitFactor,
            int totalTrades,
            double buyAndHoldReturn) {
        this.totalReturn = totalReturn;
        this.maxDrawdown = maxDrawdown;
        this.winRate = winRate;
        this.sharpeRatio = sharpeRatio;
        this.calmarRatio = calmarRatio;
        this.profitFactor = profitFactor;
        this.totalTrades = totalTrades;
        this.buyAndHoldReturn = buyAndHoldReturn;
    }

    public double getTotalReturn() { return totalReturn; }
    public double getMaxDrawdown() { return maxDrawdown; }
    public double getWinRate() { return winRate; }
    public double getSharpeRatio() { return sharpeRatio; }
    public double getCalmarRatio() { return calmarRatio; }
    public double getProfitFactor() { return profitFactor; }
    public int getTotalTrades() { return totalTrades; }
    public double getBuyAndHoldReturn() { return buyAndHoldReturn; }
}
