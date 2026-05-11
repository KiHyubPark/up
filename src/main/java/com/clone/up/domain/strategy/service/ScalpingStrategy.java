package com.clone.up.domain.strategy.service;

import com.clone.up.domain.strategy.StrategyParam;
import com.clone.up.domain.strategy.TradingStrategy;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.rules.AndRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

/**
 * RSI + MACD 스캘핑 전략 (5분봉 최적화)
 *
 * <p>매수 조건 (2가지 동시 충족)
 * 1. RSI < rsiOversold (과매도 확인, 기본 45)
 * 2. MACD가 시그널선을 상향 돌파 (반등 시작)
 *
 * <p>매도 조건
 * - 종가가 볼린저밴드 중심선(SMA 20) 이상 도달 (평균 회귀 완료)
 */
@Component
public final class ScalpingStrategy implements TradingStrategy {

    @Override
    public String name() {
        return "RSI+MACD Scalping";
    }

    @Override
    public Strategy build(BarSeries series, StrategyParam param) {
        ClosePriceIndicator close = new ClosePriceIndicator(series);

        // RSI
        RSIIndicator rsi = new RSIIndicator(close, param.rsiPeriod());

        // MACD + Signal
        MACDIndicator macd = new MACDIndicator(close, param.macdShortPeriod(), param.macdLongPeriod());
        EMAIndicator signal = new EMAIndicator(macd, param.macdSignalPeriod());

        // 볼린저밴드 중심선 (매도 기준)
        SMAIndicator sma = new SMAIndicator(close, param.bbPeriod());
        StandardDeviationIndicator stdDev = new StandardDeviationIndicator(close, param.bbPeriod());
        BollingerBandsMiddleIndicator mid = new BollingerBandsMiddleIndicator(sma);

        // 매수: RSI 과매도 + MACD 시그널 상향 돌파
        var entryRule = new AndRule(
                new UnderIndicatorRule(rsi, param.rsiOversold()),
                new CrossedUpIndicatorRule(macd, signal)
        );

        // 매도: 종가가 볼린저밴드 중심선 이상 (평균 회귀 완료)
        var exitRule = new OverIndicatorRule(close, mid);

        return new BaseStrategy(entryRule, exitRule);
    }
}
