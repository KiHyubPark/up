package com.clone.up.domain.trading.service;

import com.clone.up.config.TradingProperties;
import com.clone.up.domain.candle.entity.Candle;
import com.clone.up.domain.candle.repository.CandleRepository;
import com.clone.up.domain.indicator.service.IndicatorCalculationService;
import com.clone.up.domain.strategy.StrategyFactory;
import com.clone.up.domain.strategy.StrategyParam;
import com.clone.up.domain.trading.entity.LivePosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseTradingRecord;
import org.ta4j.core.Strategy;
import org.ta4j.core.Trade;
import org.ta4j.core.num.DecimalNum;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 자동매매 시그널 평가 서비스.
 *
 * <p>DB에서 최근 캔들을 불러와 TA4J BarSeries를 구성하고,
 * 마지막 바(최신 완성 캔들)에서 진입/청산 시그널을 평가한다.
 *
 * <p>진입 시그널: strategy.getEntryRule().isSatisfied(lastIndex, emptyRecord)
 * <p>청산 시그널: strategy.getExitRule().isSatisfied(lastIndex, reconstructedRecord)
 */
@Service
public class TradingSignalEvaluator {

    private static final Logger log = LoggerFactory.getLogger(TradingSignalEvaluator.class);

    private final CandleRepository candleRepository;
    private final IndicatorCalculationService indicatorService;
    private final StrategyFactory strategyFactory;
    private final TradingProperties properties;

    public TradingSignalEvaluator(
            CandleRepository candleRepository,
            IndicatorCalculationService indicatorService,
            StrategyFactory strategyFactory,
            TradingProperties properties) {
        this.candleRepository = candleRepository;
        this.indicatorService = indicatorService;
        this.strategyFactory = strategyFactory;
        this.properties = properties;
    }

    /**
     * 진입 시그널 여부를 평가한다.
     *
     * @return true이면 매수 진입 시그널 발생
     */
    @Transactional(readOnly = true)
    public boolean isEntrySignal() {
        EvalContext ctx = buildContext();
        if (ctx == null) return false;

        BaseTradingRecord emptyRecord = new BaseTradingRecord();
        boolean signal = ctx.strategy().getEntryRule().isSatisfied(ctx.lastIndex(), emptyRecord);

        log.debug("진입 시그널 평가 — market={}, lastIndex={}, signal={}",
                properties.getMarket(), ctx.lastIndex(), signal);
        return signal;
    }

    /**
     * 청산 시그널 여부를 평가한다.
     *
     * <p>기존 오픈 포지션의 진입 정보를 이용해 BaseTradingRecord를 재구성한 뒤 평가한다.
     *
     * @param openPosition 현재 오픈 포지션
     * @return true이면 매도 청산 시그널 발생
     */
    @Transactional(readOnly = true)
    public boolean isExitSignal(LivePosition openPosition) {
        EvalContext ctx = buildContext();
        if (ctx == null) return false;

        // 진입 바 인덱스 찾기: entryTime과 일치하는 바
        int entryBarIndex = findBarIndex(ctx.series(), openPosition.getEntryTime());

        if (entryBarIndex < 0) {
            log.warn("진입 바를 찾을 수 없음 — entryTime={}, 청산 신호 없음으로 처리",
                    openPosition.getEntryTime());
            return false;
        }

        // 진입 기록 재구성 (TA4J가 청산 규칙 평가에 사용)
        BaseTradingRecord record = new BaseTradingRecord();
        record.enter(entryBarIndex,
                DecimalNum.valueOf(openPosition.getEntryPrice()),
                DecimalNum.valueOf(openPosition.getQuantity()));

        boolean signal = ctx.strategy().getExitRule().isSatisfied(ctx.lastIndex(), record);

        log.debug("청산 시그널 평가 — market={}, entryBarIndex={}, lastIndex={}, signal={}",
                properties.getMarket(), entryBarIndex, ctx.lastIndex(), signal);
        return signal;
    }

    /**
     * 마지막 완성 캔들의 종가를 반환한다.
     */
    @Transactional(readOnly = true)
    public BigDecimal lastClosePrice() {
        return candleRepository
                .findTopByMarketAndCandleTypeOrderByCandleTimeDesc(
                        properties.getMarket(), properties.getCandleType())
                .map(Candle::getClosePrice)
                .orElse(BigDecimal.ZERO);
    }

    // ── private ─────────────────────────────────────────────────────────────

    private EvalContext buildContext() {
        List<Candle> candles = loadCandles();
        if (candles.size() < properties.getCandleWarmupCount()) {
            log.warn("캔들 수 부족 — required={}, actual={}",
                    properties.getCandleWarmupCount(), candles.size());
            return null;
        }

        BarSeries series = indicatorService.buildBarSeries(
                properties.getMarket() + "_" + properties.getCandleType(), candles);

        StrategyParam param = properties.resolvedParam();
        Strategy strategy = strategyFactory.build(properties.getStrategyType(), series, param);

        int lastIndex = series.getEndIndex();
        return new EvalContext(series, strategy, lastIndex);
    }

    private List<Candle> loadCandles() {
        // 최근 N개만 로드 (워밍업 + 여유분)
        int limit = properties.getCandleWarmupCount() + 50;
        LocalDateTime to = LocalDateTime.now();
        LocalDateTime from = to.minusDays(limit); // 대략적인 from 범위
        return candleRepository
                .findByMarketAndCandleTypeAndCandleTimeBetweenOrderByCandleTimeAsc(
                        properties.getMarket(), properties.getCandleType(), from, to);
    }

    private int findBarIndex(BarSeries series, LocalDateTime targetTime) {
        for (int i = series.getBeginIndex(); i <= series.getEndIndex(); i++) {
            LocalDateTime barTime = series.getBar(i).getEndTime().toLocalDateTime();
            if (!barTime.isBefore(targetTime) && barTime.isBefore(targetTime.plusMinutes(1))) {
                return i;
            }
        }
        return -1;
    }

    private record EvalContext(BarSeries series, Strategy strategy, int lastIndex) {}
}
