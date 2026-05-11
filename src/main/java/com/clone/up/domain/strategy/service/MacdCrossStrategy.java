package com.clone.up.domain.strategy.service;

import com.clone.up.domain.strategy.StrategyParam;
import com.clone.up.domain.strategy.TradingStrategy;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;

@Component
public final class MacdCrossStrategy implements TradingStrategy {

    @Override
    public String name() {
        return "MACD Cross";
    }

    @Override
    public Strategy build(BarSeries series, StrategyParam param) {
        MACDIndicator macd = new MACDIndicator(
                new ClosePriceIndicator(series),
                param.macdShortPeriod(),
                param.macdLongPeriod()
        );
        EMAIndicator signal = new EMAIndicator(macd, param.macdSignalPeriod());

        // 진입: MACD가 시그널선을 상향 돌파
        // 청산: MACD가 시그널선을 하향 돌파
        return new BaseStrategy(
                new CrossedUpIndicatorRule(macd, signal),
                new CrossedDownIndicatorRule(macd, signal)
        );
    }
}
