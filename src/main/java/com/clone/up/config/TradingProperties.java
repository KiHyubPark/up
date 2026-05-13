package com.clone.up.config;

import com.clone.up.domain.strategy.StrategyParam;
import com.clone.up.domain.trading.entity.TradingMode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 자동매매 설정.
 *
 * <p>application.yaml의 {@code trading.*} 속성을 바인딩한다.
 * 캔들 타입과 전략 타입은 각 전략 구현체({@link com.clone.up.domain.strategy.TradingStrategy})에서 정의한다.
 *
 * <pre>
 * trading:
 *   mode: PAPER           # LIVE | PAPER
 *   market: KRW-BTC
 *   initial-capital: 1000000
 *   invest-ratio: 1.0
 *   daily-loss-limit-percent: 3.0
 *   candle-warmup-count: 200
 *   ema-short-period: 5
 *   ema-long-period: 20
 *   rsi-period: 7
 *   rsi-momentum: 50
 *   atr-period: 14
 *   atr-stop-multiplier: 1.0
 *   atr-take-multiplier: 2.0
 *   adx-period: 14
 *   adx-threshold: 25
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "trading")
public class TradingProperties {

    private TradingMode mode = TradingMode.PAPER;
    private String market = "KRW-BTC";
    private BigDecimal initialCapital = BigDecimal.valueOf(1_000_000);
    private double investRatio = 1.0;
    private double dailyLossLimitPercent = 3.0;
    private int candleWarmupCount = 200;
    private boolean schedulerEnabled = false;

    // ── 스캘핑 전략 파라미터 ──────────────────────────────────────────────────

    private int emaShortPeriod    = 5;
    private int emaLongPeriod     = 20;
    private int rsiPeriod         = 7;
    private int rsiMomentum       = 50;
    private int atrPeriod         = 14;
    private double atrStopMultiplier = 1.0;
    private double atrTakeMultiplier = 2.0;
    private int adxPeriod         = 14;
    private int adxThreshold      = 25;
    private int volumeSmaPeriod   = 20;
    private double volumeMultiplier = 1.2;
    private int htfEmaPeriod      = 288;   // 288 × 5min ≈ 24H 추세 프록시
    private int tradeStartHour    = 9;     // KST 09:00 진입 허용 시작
    private int tradeEndHour      = 22;    // KST 22:00 진입 허용 종료

    public StrategyParam resolvedParam() {
        return new StrategyParam(
                emaShortPeriod, emaLongPeriod,
                rsiPeriod, rsiMomentum,
                atrPeriod, atrStopMultiplier, atrTakeMultiplier,
                adxPeriod, adxThreshold,
                volumeSmaPeriod, volumeMultiplier,
                htfEmaPeriod, tradeStartHour, tradeEndHour
        );
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public TradingMode getMode() { return mode; }
    public void setMode(TradingMode mode) { this.mode = mode; }

    public String getMarket() { return market; }
    public void setMarket(String market) { this.market = market; }

    public BigDecimal getInitialCapital() { return initialCapital; }
    public void setInitialCapital(BigDecimal initialCapital) { this.initialCapital = initialCapital; }

    public double getInvestRatio() { return investRatio; }
    public void setInvestRatio(double investRatio) { this.investRatio = investRatio; }

    public double getDailyLossLimitPercent() { return dailyLossLimitPercent; }
    public void setDailyLossLimitPercent(double v) { this.dailyLossLimitPercent = v; }

    public int getCandleWarmupCount() { return candleWarmupCount; }
    public void setCandleWarmupCount(int v) { this.candleWarmupCount = v; }

    public boolean isSchedulerEnabled() { return schedulerEnabled; }
    public void setSchedulerEnabled(boolean v) { this.schedulerEnabled = v; }

    public int getEmaShortPeriod() { return emaShortPeriod; }
    public void setEmaShortPeriod(int v) { this.emaShortPeriod = v; }

    public int getEmaLongPeriod() { return emaLongPeriod; }
    public void setEmaLongPeriod(int v) { this.emaLongPeriod = v; }

    public int getRsiPeriod() { return rsiPeriod; }
    public void setRsiPeriod(int v) { this.rsiPeriod = v; }

    public int getRsiMomentum() { return rsiMomentum; }
    public void setRsiMomentum(int v) { this.rsiMomentum = v; }

    public int getAtrPeriod() { return atrPeriod; }
    public void setAtrPeriod(int v) { this.atrPeriod = v; }

    public double getAtrStopMultiplier() { return atrStopMultiplier; }
    public void setAtrStopMultiplier(double v) { this.atrStopMultiplier = v; }

    public double getAtrTakeMultiplier() { return atrTakeMultiplier; }
    public void setAtrTakeMultiplier(double v) { this.atrTakeMultiplier = v; }

    public int getAdxPeriod() { return adxPeriod; }
    public void setAdxPeriod(int v) { this.adxPeriod = v; }

    public int getAdxThreshold() { return adxThreshold; }
    public void setAdxThreshold(int v) { this.adxThreshold = v; }

    public int getVolumeSmaPeriod() { return volumeSmaPeriod; }
    public void setVolumeSmaPeriod(int v) { this.volumeSmaPeriod = v; }

    public double getVolumeMultiplier() { return volumeMultiplier; }
    public void setVolumeMultiplier(double v) { this.volumeMultiplier = v; }

    public int getHtfEmaPeriod() { return htfEmaPeriod; }
    public void setHtfEmaPeriod(int v) { this.htfEmaPeriod = v; }

    public int getTradeStartHour() { return tradeStartHour; }
    public void setTradeStartHour(int v) { this.tradeStartHour = v; }

    public int getTradeEndHour() { return tradeEndHour; }
    public void setTradeEndHour(int v) { this.tradeEndHour = v; }
}
