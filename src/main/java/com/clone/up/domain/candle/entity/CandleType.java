package com.clone.up.domain.candle.entity;

import java.util.Optional;

public enum CandleType {
    MINUTE_1(1),
    MINUTE_5(5),
    MINUTE_15(15),
    MINUTE_60(60),
    MINUTE_240(240),
    DAY(null),
    WEEK(null);

    private final Integer unit;

    CandleType(Integer unit) {
        this.unit = unit;
    }

    public Optional<Integer> getUnit() {
        return Optional.ofNullable(unit);
    }

    public boolean isMinute() {
        return unit != null;
    }
}
