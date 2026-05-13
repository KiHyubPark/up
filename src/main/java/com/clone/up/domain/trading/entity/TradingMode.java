package com.clone.up.domain.trading.entity;

public enum TradingMode {
    /** 실제 포지션을 열고 닫는다. API 연결 후 실제 주문도 실행 */
    LIVE,
    /** 시그널만 로그로 기록한다. 실제 포지션 변경 없음 */
    PAPER;

    public boolean isLive() {
        return this == LIVE;
    }
}
