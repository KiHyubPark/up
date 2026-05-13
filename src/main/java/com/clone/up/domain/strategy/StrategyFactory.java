package com.clone.up.domain.strategy;

import com.clone.up.domain.candle.entity.CandleType;
import com.clone.up.domain.strategy.service.ScalpingStrategy;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Strategy;

@Component
public class StrategyFactory {

    private final ScalpingStrategy scalping;

    public StrategyFactory(ScalpingStrategy scalping) {
        this.scalping = scalping;
    }

    public Strategy build(StrategyType type, BarSeries series, StrategyParam param) {
        return scalping.build(series, param);
    }

    public String nameOf(StrategyType type) {
        return scalping.name();
    }

    /** 현재 활성 전략이 사용하는 캔들 타입 */
    public CandleType candleType() {
        return scalping.candleType();
    }

    /** 현재 활성 전략의 타입 */
    public StrategyType strategyType() {
        return scalping.type();
    }
}
