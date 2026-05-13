package com.clone.up.domain.strategy;

import com.clone.up.domain.candle.entity.CandleType;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Strategy;

public interface TradingStrategy {

    String name();

    /** 이 전략이 사용하는 캔들 타입 */
    CandleType candleType();

    /** 이 전략의 타입 식별자 */
    StrategyType type();

    Strategy build(BarSeries series, StrategyParam param);
}
