package com.clone.up.domain.strategy.service;

import com.clone.up.domain.candle.entity.CandleType;
import com.clone.up.domain.strategy.StrategyParam;
import com.clone.up.domain.strategy.StrategyType;
import com.clone.up.domain.strategy.TradingStrategy;
import com.clone.up.domain.strategy.rule.AtrStopLossRule;
import com.clone.up.domain.strategy.rule.AtrTakeProfitRule;
import com.clone.up.domain.strategy.rule.CooldownRule;
import com.clone.up.domain.strategy.rule.TimeOfDayRule;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.adx.ADXIndicator;
import org.ta4j.core.indicators.adx.PlusDIIndicator;
import org.ta4j.core.indicators.adx.MinusDIIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.TransformIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.rules.AndRule;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.OrRule;

/**
 * 스캘핑 전략 v2 (EMA 크로스 청산 제거):
 * EMA 추세 추종 + RSI 모멘텀 + ADX 강도 + DI 방향성 + 볼륨 스파이크 + ATR 손익 관리
 * + HTF EMA(288) 일봉 추세 필터 + 시간대 필터 (KST 09~22)
 *
 * <p>진입 조건 (8가지 동시 충족)
 * 1. 시간대: KST 09:00~22:00 — 업비트 고유동성 구간만 거래
 * 2. HTF 추세: 가격 > EMA(288) ≈ 24H MA — 일봉 상승 구간만 롱 허용
 * 3. EMA(9) > EMA(21) — 단기 상승 추세 확인
 * 4. 가격 > EMA(9) — 가격이 단기 추세선 위에 있음
 * 5. RSI > 50 — 모멘텀 방향 확인
 * 6. ADX > 25 — 추세 강도 확인, 횡보장 진입 차단
 * 7. +DI > -DI — 상승 방향성 확인
 * 8. Volume > SMA(Volume, 20) × 1.2 — 볼륨 스파이크 (허위 신호 제거)
 * 9. 쿨다운 5바 — 과매매 방지
 *
 * <p>청산 조건 (하나라도 충족 시 즉시 청산)
 * 1. ATR 손절: 진입가 - atrStopMultiplier×ATR
 * 2. ATR 익절: 진입가 + atrTakeMultiplier×ATR
 *
 * <p>v2 변경: EMA 역전(EMA9 < EMA21) 청산 제거.
 * 해당 룰은 후행 지표 특성으로 익절 전에 수익을 반납하는 조기 청산을 유발하여
 * 승률을 26% 수준으로 낮추는 주요 원인이었다.
 * ATR 손익 레벨만으로 청산하여 각 거래가 명확한 R:R 기준에 따라 종결되도록 한다.
 */
@Component
public final class ScalpingStrategy implements TradingStrategy {

    @Override
    public String name() {
        return "EMA Scalping";
    }

    @Override
    public CandleType candleType() {
        return CandleType.MINUTE_5;
    }

    @Override
    public StrategyType type() {
        return StrategyType.SCALPING;
    }

    @Override
    public Strategy build(BarSeries series, StrategyParam param) {
        ClosePriceIndicator close = new ClosePriceIndicator(series);

        // 단기/장기 EMA (추세 방향 판단)
        EMAIndicator emaShort = new EMAIndicator(close, param.emaShortPeriod());
        EMAIndicator emaLong  = new EMAIndicator(close, param.emaLongPeriod());

        // RSI (모멘텀 방향 확인 — 50 기준선)
        RSIIndicator rsi = new RSIIndicator(close, param.rsiPeriod());

        // ATR (손익 범위 계산)
        ATRIndicator atr = new ATRIndicator(series, param.atrPeriod());

        // ADX + DI (추세 강도 + 방향성 필터)
        ADXIndicator    adx     = new ADXIndicator(series, param.adxPeriod());
        PlusDIIndicator plusDI  = new PlusDIIndicator(series, param.adxPeriod());
        MinusDIIndicator minusDI = new MinusDIIndicator(series, param.adxPeriod());

        // 볼륨 스파이크 필터 (허위 신호 제거)
        VolumeIndicator volume    = new VolumeIndicator(series);
        SMAIndicator    volumeSma = new SMAIndicator(volume, param.volumeSmaPeriod());

        // HTF(상위 타임프레임) 추세 프록시 — EMA(288) ≈ 24H 이동평균 (5분봉 288개 = 24시간)
        EMAIndicator htfEma = new EMAIndicator(close, param.htfEmaPeriod());

        // 진입 조건 구성
        Rule timeFilter   = new TimeOfDayRule(series, param.tradeStartHour(), param.tradeEndHour());  // KST 09~22
        Rule htfFilter    = new OverIndicatorRule(close, htfEma);                                      // 가격 > 24H EMA (일봉 상승)
        Rule emaTrend     = new OverIndicatorRule(emaShort, emaLong);                                  // 단기 > 장기 EMA
        Rule priceAboveEma = new OverIndicatorRule(close, emaShort);                                   // 가격 > 단기 EMA
        Rule rsiFilter    = new OverIndicatorRule(rsi, DecimalNum.valueOf(param.rsiMomentum()));        // RSI > 50
        Rule adxFilter    = new OverIndicatorRule(adx, DecimalNum.valueOf(param.adxThreshold()));       // ADX > 25
        Rule diFilter     = new OverIndicatorRule(plusDI, minusDI);                                    // +DI > -DI
        Rule volumeFilter = new OverIndicatorRule(volume,
                TransformIndicator.multiply(volumeSma, param.volumeMultiplier()));                      // 볼륨 스파이크
        Rule cooldown     = new CooldownRule(5);                                                        // 5바 쿨다운

        Rule entryRule = new AndRule(timeFilter,
                         new AndRule(htfFilter,
                         new AndRule(emaTrend,
                         new AndRule(priceAboveEma,
                         new AndRule(rsiFilter,
                         new AndRule(adxFilter,
                         new AndRule(diFilter,
                         new AndRule(volumeFilter, cooldown))))))));

        // 청산 1: ATR 손절
        Rule stopLoss = new AtrStopLossRule(close, atr, param.atrStopMultiplier());

        // 청산 2: ATR 익절
        Rule takeProfit = new AtrTakeProfitRule(close, atr, param.atrTakeMultiplier());

        Rule exitRule = new OrRule(stopLoss, takeProfit);

        return new BaseStrategy(entryRule, exitRule);
    }
}
