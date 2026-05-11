package com.clone.up.domain.strategy;

public record StrategyParam(
        int shortPeriod,
        int longPeriod,
        int rsiPeriod,
        int rsiOversold,
        int rsiOverbought,
        int bbPeriod,
        double bbMultiplier,
        int macdShortPeriod,
        int macdLongPeriod,
        int macdSignalPeriod
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
                9     // macdSignalPeriod
        );
    }
}
