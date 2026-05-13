package com.clone.up.domain.strategy;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StrategyParam(
        @JsonProperty("emaShortPeriod") int emaShortPeriod,
        @JsonProperty("emaLongPeriod")  int emaLongPeriod,
        @JsonProperty("rsiPeriod")      int rsiPeriod,
        @JsonProperty("rsiMomentum")    int rsiMomentum,
        @JsonProperty("atrPeriod")      int atrPeriod,
        @JsonProperty("atrStopMultiplier") double atrStopMultiplier,
        @JsonProperty("atrTakeMultiplier") double atrTakeMultiplier,
        @JsonProperty("adxPeriod")      int adxPeriod,
        @JsonProperty("adxThreshold")   int adxThreshold,
        @JsonProperty("volumeSmaPeriod") int volumeSmaPeriod,
        @JsonProperty("volumeMultiplier") double volumeMultiplier,
        @JsonProperty("htfEmaPeriod")   int htfEmaPeriod,
        @JsonProperty("tradeStartHour") int tradeStartHour,
        @JsonProperty("tradeEndHour")   int tradeEndHour
) {
    /**
     * EMA 스캘핑 기본 파라미터 (Perplexity 리서치 기반 최적화)
     *
     * <p>EMA 9/21: 노이즈 줄이면서 단기 추세 포착
     * <p>RSI 14/50: 과매수 필터 대신 모멘텀 방향 확인 (승률 개선)
     * <p>ATR 손절 1.5배 / 익절 2.5배: BTC 변동성에 맞춘 손익비
     * <p>ADX 14/25 + +DI/-DI: 추세 강도 + 방향성 동시 확인
     * <p>Volume > SMA(20)×1.2: 볼륨 스파이크로 허위 신호 제거
     * <p>HTF EMA(288) ≈ 24H 이동평균: 일봉 상승 구간만 롱 허용 (하락장 진입 차단)
     * <p>시간대 필터 KST 09~22: 업비트 고유동성 시간대만 거래 (허위 신호 제거)
     */
    public static StrategyParam defaults() {
        return new StrategyParam(
                9,    // emaShortPeriod
                21,   // emaLongPeriod
                14,   // rsiPeriod
                50,   // rsiMomentum (모멘텀 방향 확인)
                14,   // atrPeriod
                1.5,  // atrStopMultiplier
                2.5,  // atrTakeMultiplier
                14,   // adxPeriod
                25,   // adxThreshold
                20,   // volumeSmaPeriod
                1.2,  // volumeMultiplier
                288,  // htfEmaPeriod (288 × 5min = 24H 추세 프록시)
                9,    // tradeStartHour (KST 09:00)
                22    // tradeEndHour   (KST 22:00)
        );
    }
}
