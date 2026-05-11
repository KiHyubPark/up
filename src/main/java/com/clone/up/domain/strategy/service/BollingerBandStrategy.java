package com.clone.up.domain.strategy.service;

import com.clone.up.domain.strategy.StrategyParam;
import com.clone.up.domain.strategy.TradingStrategy;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;

@Component
public final class BollingerBandStrategy implements TradingStrategy {

    @Override
    public String name() {
        return "Bollinger Band";
    }

    @Override
    public Strategy build(BarSeries series, StrategyParam param) {
        ClosePriceIndicator close = new ClosePriceIndicator(series);
        SMAIndicator sma = new SMAIndicator(close, param.bbPeriod());
        StandardDeviationIndicator stdDev = new StandardDeviationIndicator(close, param.bbPeriod());

        BollingerBandsMiddleIndicator mid = new BollingerBandsMiddleIndicator(sma);
        BollingerBandsUpperIndicator upper = new BollingerBandsUpperIndicator(
                mid, stdDev, series.numOf(param.bbMultiplier()));
        BollingerBandsLowerIndicator lower = new BollingerBandsLowerIndicator(
                mid, stdDev, series.numOf(param.bbMultiplier()));

        // 진입: 종가가 하단 밴드를 하향 돌파 (과매도)
        // 청산: 종가가 상단 밴드를 상향 돌파 (과매수)
        return new BaseStrategy(
                new CrossedDownIndicatorRule(close, lower),
                new CrossedUpIndicatorRule(close, upper)
        );
    }
}
