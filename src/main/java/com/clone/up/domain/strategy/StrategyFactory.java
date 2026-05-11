package com.clone.up.domain.strategy;

import com.clone.up.domain.strategy.service.BollingerBandStrategy;
import com.clone.up.domain.strategy.service.GoldenCrossStrategy;
import com.clone.up.domain.strategy.service.MacdCrossStrategy;
import com.clone.up.domain.strategy.service.RsiOversoldStrategy;
import com.clone.up.domain.strategy.service.ScalpingStrategy;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Strategy;

@Component
public class StrategyFactory {

    private final GoldenCrossStrategy goldenCross;
    private final RsiOversoldStrategy rsiOversold;
    private final MacdCrossStrategy macdCross;
    private final BollingerBandStrategy bollingerBand;
    private final ScalpingStrategy scalping;

    public StrategyFactory(
            GoldenCrossStrategy goldenCross,
            RsiOversoldStrategy rsiOversold,
            MacdCrossStrategy macdCross,
            BollingerBandStrategy bollingerBand,
            ScalpingStrategy scalping) {
        this.goldenCross = goldenCross;
        this.rsiOversold = rsiOversold;
        this.macdCross = macdCross;
        this.bollingerBand = bollingerBand;
        this.scalping = scalping;
    }

    public Strategy build(StrategyType type, BarSeries series, StrategyParam param) {
        TradingStrategy strategy = switch (type) {
            case GOLDEN_CROSS -> goldenCross;
            case RSI_OVERSOLD -> rsiOversold;
            case MACD_CROSS -> macdCross;
            case BOLLINGER_BAND -> bollingerBand;
            case SCALPING -> scalping;
        };
        return strategy.build(series, param);
    }

    public String nameOf(StrategyType type) {
        return switch (type) {
            case GOLDEN_CROSS -> goldenCross.name();
            case RSI_OVERSOLD -> rsiOversold.name();
            case MACD_CROSS -> macdCross.name();
            case BOLLINGER_BAND -> bollingerBand.name();
            case SCALPING -> scalping.name();
        };
    }
}
