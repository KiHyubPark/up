package com.clone.up.domain.strategy.service;

import com.clone.up.domain.strategy.StrategyParam;
import com.clone.up.domain.strategy.TradingStrategy;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.AndRule;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

/**
 * RSI + MACD + ATR 스캘핑 전략
 *
 * <p>매수 조건 (3가지 동시 충족)
 * 1. RSI < rsiOversold (과매도 확인, 기본 30)
 * 2. MACD가 시그널선 위 (상승 모멘텀 확인)
 * 3. ATR > SMA(ATR, atrPeriod*2) — 평균 이상 변동성 (저변동 구간 필터)
 *
 * <p>매도 조건
 * - RSI > RSI_EXIT_LEVEL(50): 과매도 해소, 평균 회귀 완료
 */
@Component
public final class ScalpingStrategy implements TradingStrategy {

    /** RSI가 이 값을 넘으면 청산 (과매도 해소 기준) */
    private static final int RSI_EXIT_LEVEL = 50;

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

        // ATR 필터: 현재 ATR > ATR의 이동평균 → 평균 이상 변동성 구간에서만 진입
        ATRIndicator atr = new ATRIndicator(series, param.atrPeriod());
        SMAIndicator atrSma = new SMAIndicator(atr, param.atrPeriod() * 2);

        // 매수: RSI 과매도 + MACD 상승 모멘텀 + ATR 필터 (저변동 구간 제외)
        var entryRule = new AndRule(
                new AndRule(
                        new UnderIndicatorRule(rsi, param.rsiOversold()),
                        new OverIndicatorRule(macd, signal)
                ),
                new OverIndicatorRule(atr, atrSma)
        );

        // 매도: RSI > 50 (과매도 해소, 평균 회귀 완료)
        var exitRule = new OverIndicatorRule(rsi, RSI_EXIT_LEVEL);

        return new BaseStrategy(entryRule, exitRule);
    }
}
