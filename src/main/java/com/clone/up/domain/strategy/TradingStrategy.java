package com.clone.up.domain.strategy;

import org.ta4j.core.BarSeries;
import org.ta4j.core.Strategy;

public interface TradingStrategy {

    String name();

    Strategy build(BarSeries series, StrategyParam param);
}
