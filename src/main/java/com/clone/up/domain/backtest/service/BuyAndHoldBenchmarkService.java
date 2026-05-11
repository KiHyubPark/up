package com.clone.up.domain.backtest.service;

import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;

import java.math.BigDecimal;

@Service
public class BuyAndHoldBenchmarkService {

    /**
     * Buy & Hold 수익률 계산 (첫 번째 봉 시가에 매수, 마지막 봉 종가에 매도)
     */
    public double calculate(BarSeries series, BigDecimal initialCapital) {
        if (series.getBarCount() < 2) {
            return 0.0;
        }
        double entryPrice = series.getFirstBar().getOpenPrice().doubleValue();
        double exitPrice = series.getLastBar().getClosePrice().doubleValue();

        if (entryPrice <= 0) {
            return 0.0;
        }
        return (exitPrice - entryPrice) / entryPrice;
    }
}
