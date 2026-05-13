package com.clone.up.domain.strategy.rule;

import org.ta4j.core.BarSeries;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.rules.AbstractRule;

import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * 시간대 필터 — 지정한 KST 시간 범위 안에서만 진입 허용
 *
 * <p>업비트 KRW-BTC 특화: 거래량의 65%가 KST 09:00~13:00에 집중,
 * 저유동성 새벽 시간대(KST 22:00~09:00)의 허위 신호를 차단한다.
 *
 * <p>기본값: KST 09:00 ~ 22:00 (허용 창)
 */
public class TimeOfDayRule extends AbstractRule {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final BarSeries series;
    private final int startHour;  // inclusive
    private final int endHour;    // exclusive

    /**
     * @param series    바 시리즈 (바의 종료 시각 조회용)
     * @param startHour 허용 시작 시각 (KST, inclusive) — 예: 9
     * @param endHour   허용 종료 시각 (KST, exclusive) — 예: 22
     */
    public TimeOfDayRule(BarSeries series, int startHour, int endHour) {
        this.series   = series;
        this.startHour = startHour;
        this.endHour   = endHour;
    }

    @Override
    public boolean isSatisfied(int index, TradingRecord tradingRecord) {
        ZonedDateTime kst = series.getBar(index).getEndTime().withZoneSameInstant(KST);
        int hour = kst.getHour();
        boolean satisfied = hour >= startHour && hour < endHour;
        traceIsSatisfied(index, satisfied);
        return satisfied;
    }
}
