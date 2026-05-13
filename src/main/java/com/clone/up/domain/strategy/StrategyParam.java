package com.clone.up.domain.strategy;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StrategyParam(
        @JsonProperty("shortPeriod") int shortPeriod,
        @JsonProperty("longPeriod") int longPeriod,
        @JsonProperty("rsiPeriod") int rsiPeriod,
        @JsonProperty("rsiOversold") int rsiOversold,
        @JsonProperty("rsiOverbought") int rsiOverbought,
        @JsonProperty("bbPeriod") int bbPeriod,
        @JsonProperty("bbMultiplier") double bbMultiplier,
        @JsonProperty("macdShortPeriod") int macdShortPeriod,
        @JsonProperty("macdLongPeriod") int macdLongPeriod,
        @JsonProperty("macdSignalPeriod") int macdSignalPeriod,
        @JsonProperty("atrPeriod") int atrPeriod
) {
    public static StrategyParam defaults() {
        return new StrategyParam(
                5,    // shortPeriod (SMA 단기)
                20,   // longPeriod (SMA 장기)
                14,   // rsiPeriod
                30,   // rsiOversold
                70,   // rsiOverbought
                20,   // bbPeriod
                2.0,  // bbMultiplier
                12,   // macdShortPeriod
                26,   // macdLongPeriod
                9,    // macdSignalPeriod
                14    // atrPeriod
        );
    }

    /**
     * SCALPING 전략 최적 파라미터 (15분봉 KRW-BTC 백테스팅 기준)
     *
     * <p>검증 기간: 2025-05-13 ~ 2026-05-13 (1년, MINUTE_15)
     * <p>결과: 수익률 +21.38%, 승률 93.9%, 샤프 1.11, 칼마 3.21, PF 16.98
     * <p>vs BTC 보유: -17.7% (약 39%p 알파)
     */
    public static StrategyParam scalpingOptimized() {
        return new StrategyParam(
                5,    // shortPeriod
                20,   // longPeriod
                14,   // rsiPeriod
                35,   // rsiOversold — 30(신호 부족) vs 40(낙폭 큼) 사이 최적값
                70,   // rsiOverbought
                20,   // bbPeriod
                2.0,  // bbMultiplier
                12,   // macdShortPeriod
                26,   // macdLongPeriod
                9,    // macdSignalPeriod
                14    // atrPeriod — ATR > SMA(ATR,28) 필터로 저변동 구간 제외
        );
    }
}
