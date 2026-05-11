package com.clone.up.domain.strategy.service;

import com.clone.up.domain.strategy.StrategyParam;
import com.clone.up.domain.strategy.TradingStrategy;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

@Component
public final class RsiOversoldStrategy implements TradingStrategy {

    @Override
    public String name() {
        return "RSI Oversold";
    }

    @Override
    public Strategy build(BarSeries series, StrategyParam param) {
        RSIIndicator rsi = new RSIIndicator(new ClosePriceIndicator(series), param.rsiPeriod());

        // 진입: RSI가 과매도 기준선을 하향 돌파 (oversold 진입)
        // 청산: RSI가 과매수 기준선을 상향 돌파 (overbought 도달)
        return new BaseStrategy(
                new CrossedDownIndicatorRule(rsi, param.rsiOversold()),
                new CrossedUpIndicatorRule(rsi, param.rsiOverbought())
        );
    }
}
