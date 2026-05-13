package com.clone.up.domain.trading.entity;

public enum SignalType {
    BUY_SIGNAL,   // 매수 시그널 발생
    SELL_SIGNAL,  // 매도 시그널 발생
    HALTED        // 리스크 한도 / 긴급 중단으로 인해 시그널 억제됨
}
