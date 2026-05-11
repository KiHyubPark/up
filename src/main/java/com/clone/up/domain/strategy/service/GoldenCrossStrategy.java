package com.clone.up.domain.strategy.service;

import com.clone.up.domain.strategy.StrategyParam;
import com.clone.up.domain.strategy.TradingStrategy;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;

@Component
public final class GoldenCrossStrategy implements TradingStrategy {

    @Override
    public String name() {
        return "Golden Cross";
    }

    @Override
    public Strategy build(BarSeries series, StrategyParam param) {
        ClosePriceIndicator close = new ClosePriceIndicator(series);
        SMAIndicator shortSma = new SMAIndicator(close, param.shortPeriod());
        SMAIndicator longSma = new SMAIndicator(close, param.longPeriod());

        // 진입: 단기 SMA가 장기 SMA를 상향 돌파
        // 청산: 단기 SMA가 장기 SMA를 하향 돌파
        return new BaseStrategy(
                new CrossedUpIndicatorRule(shortSma, longSma),
                new CrossedDownIndicatorRule(shortSma, longSma)
        );
    }
}
