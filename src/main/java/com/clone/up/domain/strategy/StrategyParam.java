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
        @JsonProperty("atrPeriod") int atrPeriod,
        @JsonProperty("atrStopMultiplier") double atrStopMultiplier,
        @JsonProperty("stopLossPercent") double stopLossPercent
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
                14,   // atrPeriod
                0.0,  // atrStopMultiplier (0 = 비활성)
                0.0   // stopLossPercent   (0 = 비활성)
        );
    }

    /**
     * SCALPING 전략 최적 파라미터 — 고정 10% 안전망 포함
     *
     * <p>검증 기간: 2025-05-13 ~ 2026-05-13 (1년, MINUTE_15)
     * <p>결과: 수익률 +21.38%, 승률 93.9%, 샤프 1.11, 칼마 3.21, PF 16.98
     * <p>-10% 손절은 1년간 미발동 (퍼포먼스 영향 없음) — 급락 안전망 용도
     */
    public static StrategyParam scalpingOptimized() {
        return new StrategyParam(
                5, 20, 14,
                35,   // rsiOversold — 최적값
                70, 20, 2.0, 12, 26, 9,
                14,   // atrPeriod
                0.0,  // atrStopMultiplier (비활성)
                10.0  // stopLossPercent — 급락 안전망 (-10%), 퍼포먼스 영향 없음
        );
    }

    /** ATR×1.5 손절 + 고정 5% 손절 조합 */
    public static StrategyParam scalpingAtrStop15() {
        return new StrategyParam(
                5, 20, 14, 35, 70, 20, 2.0, 12, 26, 9,
                14,   // atrPeriod
                1.5,  // atrStopMultiplier
                5.0   // stopLossPercent (%)
        );
    }

    /** ATR×2.0 손절 + 고정 5% 손절 조합 */
    public static StrategyParam scalpingAtrStop20() {
        return new StrategyParam(
                5, 20, 14, 35, 70, 20, 2.0, 12, 26, 9,
                14,   // atrPeriod
                2.0,  // atrStopMultiplier
                5.0   // stopLossPercent (%)
        );
    }

    /** ATR×2.5 손절 + 고정 5% 손절 조합 */
    public static StrategyParam scalpingAtrStop25() {
        return new StrategyParam(
                5, 20, 14, 35, 70, 20, 2.0, 12, 26, 9,
                14,   // atrPeriod
                2.5,  // atrStopMultiplier
                5.0   // stopLossPercent (%)
        );
    }
}
