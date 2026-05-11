package com.clone.up.domain.backtest.dto;

import com.clone.up.domain.backtest.entity.BacktestResult;
import com.clone.up.domain.backtest.entity.PerformanceMetrics;
import com.clone.up.domain.candle.entity.CandleType;
import com.clone.up.domain.strategy.StrategyType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BacktestResponse(
        Long id,
        String market,
        CandleType candleType,
        StrategyType strategyType,
        LocalDateTime startDate,
        LocalDateTime endDate,
        BigDecimal initialCapital,
        BigDecimal finalValue,
        double totalReturn,
        double maxDrawdown,
        double winRate,
        double sharpeRatio,
        double calmarRatio,
        double profitFactor,
        int totalTrades,
        double buyAndHoldReturn,
        LocalDateTime createdAt
) {
    public static BacktestResponse from(BacktestResult result) {
        PerformanceMetrics m = result.getMetrics();
        return new BacktestResponse(
                result.getId(),
                result.getMarket(),
                result.getCandleType(),
                result.getStrategyType(),
                result.getStartDate(),
                result.getEndDate(),
                result.getInitialCapital(),
                result.getFinalValue(),
                m.getTotalReturn(),
                m.getMaxDrawdown(),
                m.getWinRate(),
                m.getSharpeRatio(),
                m.getCalmarRatio(),
                m.getProfitFactor(),
                m.getTotalTrades(),
                m.getBuyAndHoldReturn(),
                result.getCreatedAt()
        );
    }
}
