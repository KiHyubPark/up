package com.clone.up.domain.strategy.rule;

import org.ta4j.core.Position;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.AbstractRule;

/**
 * ATR 기반 익절 룰
 *
 * <p>익절가 = 진입가 + multiplier × ATR(진입 시점)
 * <p>현재가가 익절가 이상이면 청산 신호 발생
 */
public class AtrTakeProfitRule extends AbstractRule {

    private final ClosePriceIndicator close;
    private final ATRIndicator atr;
    private final double multiplier;

    public AtrTakeProfitRule(ClosePriceIndicator close, ATRIndicator atr, double multiplier) {
        this.close = close;
        this.atr = atr;
        this.multiplier = multiplier;
    }

    @Override
    public boolean isSatisfied(int index, TradingRecord tradingRecord) {
        if (tradingRecord == null || !tradingRecord.getCurrentPosition().isOpened()) {
            return false;
        }

        Position position = tradingRecord.getCurrentPosition();
        int entryIndex = position.getEntry().getIndex();

        double entryPrice = position.getEntry().getNetPrice().doubleValue();
        double atrValue = atr.getValue(entryIndex).doubleValue();
        double targetPrice = entryPrice + multiplier * atrValue;
        double currentPrice = close.getValue(index).doubleValue();

        boolean satisfied = currentPrice >= targetPrice;
        traceIsSatisfied(index, satisfied);
        return satisfied;
    }
}
