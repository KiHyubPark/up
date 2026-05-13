package com.clone.up.domain.strategy.rule;

import org.ta4j.core.TradingRecord;
import org.ta4j.core.rules.AbstractRule;

/**
 * 쿨다운 룰 — 마지막 청산 이후 최소 N개 바가 경과해야 진입 허용
 *
 * <p>과도한 재진입(overtrading)을 방지한다.
 * 1분봉 기준 cooldownBars=5 이면 마지막 청산 후 5분이 지나야 재진입 허용.
 */
public class CooldownRule extends AbstractRule {

    private final int cooldownBars;

    public CooldownRule(int cooldownBars) {
        this.cooldownBars = cooldownBars;
    }

    @Override
    public boolean isSatisfied(int index, TradingRecord tradingRecord) {
        if (tradingRecord == null || tradingRecord.getPositionCount() == 0) {
            // 이전 포지션 없음 — 쿨다운 불필요
            traceIsSatisfied(index, true);
            return true;
        }

        // 마지막으로 종료된 포지션의 청산 바 인덱스
        int lastExitIndex = tradingRecord.getLastPosition().getExit().getIndex();
        boolean satisfied = (index - lastExitIndex) >= cooldownBars;
        traceIsSatisfied(index, satisfied);
        return satisfied;
    }
}
