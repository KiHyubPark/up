package com.clone.up.domain.backtest.service;

import com.clone.up.domain.backtest.entity.PerformanceMetrics;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Position;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.analysis.criteria.MaximumDrawdownCriterion;
import org.ta4j.core.analysis.criteria.WinningPositionsRatioCriterion;
import org.ta4j.core.analysis.criteria.pnl.GrossLossCriterion;
import org.ta4j.core.analysis.criteria.pnl.GrossProfitCriterion;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class PerformanceAnalysisService {

    private final BuyAndHoldBenchmarkService benchmarkService;

    public PerformanceAnalysisService(BuyAndHoldBenchmarkService benchmarkService) {
        this.benchmarkService = benchmarkService;
    }

    public PerformanceMetrics analyze(BarSeries series, TradingRecord record, BigDecimal initialCapital) {
        int totalTrades = record.getPositionCount();

        if (totalTrades == 0) {
            double buyAndHold = benchmarkService.calculate(series, initialCapital);
            return new PerformanceMetrics(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0, buyAndHold);
        }

        // 포지션별 수익률을 복리로 누적 (getNetPrice = 수수료 반영 가격)
        double totalReturn = compoundReturn(record);

        double maxDrawdown = new MaximumDrawdownCriterion()
                .calculate(series, record)
                .doubleValue();

        double winRate = new WinningPositionsRatioCriterion()
                .calculate(series, record)
                .doubleValue();

        double sharpeRatio = calculateSharpeRatio(series, record);

        double profitFactor = calculateProfitFactor(series, record);

        double calmarRatio = maxDrawdown != 0 ? totalReturn / Math.abs(maxDrawdown) : 0.0;

        double buyAndHoldReturn = benchmarkService.calculate(series, initialCapital);

        return new PerformanceMetrics(
                totalReturn,
                maxDrawdown,
                winRate,
                sharpeRatio,
                calmarRatio,
                profitFactor,
                totalTrades,
                buyAndHoldReturn
        );
    }

    /**
     * Sharpe Ratio = 평균 포지션 수익률 / 표준편차 (무위험 수익률 0 기준)
     */
    private double calculateSharpeRatio(BarSeries series, TradingRecord record) {
        List<Double> returns = new ArrayList<>();
        for (Position pos : record.getPositions()) {
            if (!pos.isClosed()) continue;
            double entryPrice = pos.getEntry().getNetPrice().doubleValue();
            double exitPrice = pos.getExit().getNetPrice().doubleValue();
            if (entryPrice > 0) {
                returns.add((exitPrice - entryPrice) / entryPrice);
            }
        }
        if (returns.size() < 2) return 0.0;

        double mean = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = returns.stream()
                .mapToDouble(r -> Math.pow(r - mean, 2))
                .average()
                .orElse(0.0);
        double stdDev = Math.sqrt(variance);
        return stdDev != 0 ? mean / stdDev : 0.0;
    }

    private double calculateProfitFactor(BarSeries series, TradingRecord record) {
        double grossProfit = new GrossProfitCriterion()
                .calculate(series, record)
                .doubleValue();
        double grossLoss = Math.abs(new GrossLossCriterion()
                .calculate(series, record)
                .doubleValue());

        return grossLoss != 0 ? grossProfit / grossLoss : grossProfit > 0 ? Double.MAX_VALUE : 0.0;
    }

    private double compoundReturn(TradingRecord record) {
        double multiplier = 1.0;
        for (Position pos : record.getPositions()) {
            if (!pos.isClosed()) continue;
            double entryNetPrice = pos.getEntry().getNetPrice().doubleValue();
            double exitNetPrice = pos.getExit().getNetPrice().doubleValue();
            if (entryNetPrice > 0) {
                multiplier *= (exitNetPrice / entryNetPrice);
            }
        }
        return multiplier - 1.0;
    }
}
