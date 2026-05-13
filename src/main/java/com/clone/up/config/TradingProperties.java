package com.clone.up.config;

import com.clone.up.domain.candle.entity.CandleType;
import com.clone.up.domain.strategy.StrategyParam;
import com.clone.up.domain.strategy.StrategyType;
import com.clone.up.domain.trading.entity.TradingMode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 자동매매 설정.
 *
 * <p>application.yaml의 {@code trading.*} 속성을 바인딩한다.
 *
 * <pre>
 * trading:
 *   mode: PAPER           # LIVE | PAPER
 *   market: KRW-BTC
 *   candle-type: MINUTE_15
 *   strategy-type: SCALPING
 *   initial-capital: 1000000
 *   invest-ratio: 1.0       # 매수 시 자본 투입 비율 (0~1)
 *   daily-loss-limit-percent: 3.0
 *   candle-warmup-count: 200
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "trading")
public class TradingProperties {

    /** LIVE: 실매매, PAPER: 시그널 기록만 */
    private TradingMode mode = TradingMode.PAPER;

    private String market = "KRW-BTC";

    private CandleType candleType = CandleType.MINUTE_15;

    private StrategyType strategyType = StrategyType.SCALPING;

    /** 백테스팅 기준 초기 자본금 (일일 손실 한도 계산에 사용) */
    private BigDecimal initialCapital = BigDecimal.valueOf(1_000_000);

    /** 매수 시 investedAmount = initialCapital × investRatio */
    private double investRatio = 1.0;

    /** 일일 손실 한도 (%). totalPnl < -(initialCapital × percent/100) 이면 당일 거래 중단 */
    private double dailyLossLimitPercent = 3.0;

    /** BarSeries 구성에 필요한 최소 캔들 수 (지표 워밍업) */
    private int candleWarmupCount = 200;

    /** 스케줄러 활성 여부 (운영 환경에서만 true) */
    private boolean schedulerEnabled = false;

    // ── 전략 파라미터 (기본값 = scalpingOptimized) ──────────────────────────

    private int shortPeriod = 5;
    private int longPeriod = 20;
    private int rsiPeriod = 14;
    private int rsiOversold = 35;
    private int rsiOverbought = 70;
    private int bbPeriod = 20;
    private double bbMultiplier = 2.0;
    private int macdShortPeriod = 12;
    private int macdLongPeriod = 26;
    private int macdSignalPeriod = 9;
    private int atrPeriod = 14;
    private double atrStopMultiplier = 0.0;
    private double stopLossPercent = 10.0;

    public StrategyParam resolvedParam() {
        return new StrategyParam(
                shortPeriod, longPeriod,
                rsiPeriod, rsiOversold, rsiOverbought,
                bbPeriod, bbMultiplier,
                macdShortPeriod, macdLongPeriod, macdSignalPeriod,
                atrPeriod, atrStopMultiplier, stopLossPercent
        );
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public TradingMode getMode() { return mode; }
    public void setMode(TradingMode mode) { this.mode = mode; }

    public String getMarket() { return market; }
    public void setMarket(String market) { this.market = market; }

    public CandleType getCandleType() { return candleType; }
    public void setCandleType(CandleType candleType) { this.candleType = candleType; }

    public StrategyType getStrategyType() { return strategyType; }
    public void setStrategyType(StrategyType strategyType) { this.strategyType = strategyType; }

    public BigDecimal getInitialCapital() { return initialCapital; }
    public void setInitialCapital(BigDecimal initialCapital) { this.initialCapital = initialCapital; }

    public double getInvestRatio() { return investRatio; }
    public void setInvestRatio(double investRatio) { this.investRatio = investRatio; }

    public double getDailyLossLimitPercent() { return dailyLossLimitPercent; }
    public void setDailyLossLimitPercent(double dailyLossLimitPercent) {
        this.dailyLossLimitPercent = dailyLossLimitPercent;
    }

    public int getCandleWarmupCount() { return candleWarmupCount; }
    public void setCandleWarmupCount(int candleWarmupCount) { this.candleWarmupCount = candleWarmupCount; }

    public boolean isSchedulerEnabled() { return schedulerEnabled; }
    public void setSchedulerEnabled(boolean schedulerEnabled) { this.schedulerEnabled = schedulerEnabled; }

    public int getShortPeriod() { return shortPeriod; }
    public void setShortPeriod(int shortPeriod) { this.shortPeriod = shortPeriod; }

    public int getLongPeriod() { return longPeriod; }
    public void setLongPeriod(int longPeriod) { this.longPeriod = longPeriod; }

    public int getRsiPeriod() { return rsiPeriod; }
    public void setRsiPeriod(int rsiPeriod) { this.rsiPeriod = rsiPeriod; }

    public int getRsiOversold() { return rsiOversold; }
    public void setRsiOversold(int rsiOversold) { this.rsiOversold = rsiOversold; }

    public int getRsiOverbought() { return rsiOverbought; }
    public void setRsiOverbought(int rsiOverbought) { this.rsiOverbought = rsiOverbought; }

    public int getBbPeriod() { return bbPeriod; }
    public void setBbPeriod(int bbPeriod) { this.bbPeriod = bbPeriod; }

    public double getBbMultiplier() { return bbMultiplier; }
    public void setBbMultiplier(double bbMultiplier) { this.bbMultiplier = bbMultiplier; }

    public int getMacdShortPeriod() { return macdShortPeriod; }
    public void setMacdShortPeriod(int macdShortPeriod) { this.macdShortPeriod = macdShortPeriod; }

    public int getMacdLongPeriod() { return macdLongPeriod; }
    public void setMacdLongPeriod(int macdLongPeriod) { this.macdLongPeriod = macdLongPeriod; }

    public int getMacdSignalPeriod() { return macdSignalPeriod; }
    public void setMacdSignalPeriod(int macdSignalPeriod) { this.macdSignalPeriod = macdSignalPeriod; }

    public int getAtrPeriod() { return atrPeriod; }
    public void setAtrPeriod(int atrPeriod) { this.atrPeriod = atrPeriod; }

    public double getAtrStopMultiplier() { return atrStopMultiplier; }
    public void setAtrStopMultiplier(double atrStopMultiplier) { this.atrStopMultiplier = atrStopMultiplier; }

    public double getStopLossPercent() { return stopLossPercent; }
    public void setStopLossPercent(double stopLossPercent) { this.stopLossPercent = stopLossPercent; }
}
