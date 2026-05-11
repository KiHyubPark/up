package com.clone.up.domain.backtest.service;

import com.clone.up.domain.backtest.dto.BacktestRequest;
import com.clone.up.domain.backtest.dto.BacktestResponse;
import com.clone.up.domain.backtest.entity.BacktestResult;
import com.clone.up.domain.backtest.entity.PerformanceMetrics;
import com.clone.up.domain.backtest.entity.Trade;
import com.clone.up.domain.backtest.entity.TradeType;
import com.clone.up.domain.backtest.repository.BacktestResultRepository;
import com.clone.up.domain.candle.entity.Candle;
import com.clone.up.domain.candle.entity.CandleType;
import com.clone.up.domain.candle.repository.CandleRepository;
import com.clone.up.domain.indicator.service.IndicatorCalculationService;
import com.clone.up.domain.strategy.StrategyFactory;
import com.clone.up.domain.strategy.StrategyParam;
import com.clone.up.domain.strategy.StrategyType;
import com.clone.up.global.exception.ErrorCode;
import com.clone.up.global.exception.UpException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BarSeriesManager;
import org.ta4j.core.Position;
import org.ta4j.core.Strategy;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.cost.LinearTransactionCostModel;
import org.ta4j.core.cost.ZeroCostModel;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BacktestExecutionService {

    private static final int MIN_CANDLE_COUNT = 30;
    /** 업비트 KRW 마켓 거래 수수료 0.05% (매수/매도 각각 적용) */
    private static final double UPBIT_FEE_RATE = 0.0005;

    private final CandleRepository candleRepository;
    private final IndicatorCalculationService indicatorService;
    private final StrategyFactory strategyFactory;
    private final PerformanceAnalysisService performanceService;
    private final BacktestResultRepository resultRepository;

    public BacktestExecutionService(
            CandleRepository candleRepository,
            IndicatorCalculationService indicatorService,
            StrategyFactory strategyFactory,
            PerformanceAnalysisService performanceService,
            BacktestResultRepository resultRepository) {
        this.candleRepository = candleRepository;
        this.indicatorService = indicatorService;
        this.strategyFactory = strategyFactory;
        this.performanceService = performanceService;
        this.resultRepository = resultRepository;
    }

    @Transactional
    public BacktestResponse run(BacktestRequest request) {
        List<Candle> candles = candleRepository
                .findByMarketAndCandleTypeAndCandleTimeBetweenOrderByCandleTimeAsc(
                        request.market(),
                        request.candleType(),
                        request.startDate(),
                        request.endDate()
                );

        if (candles.size() < MIN_CANDLE_COUNT) {
            throw new UpException(ErrorCode.CANDLE_NOT_ENOUGH);
        }

        BarSeries series = indicatorService.buildBarSeries(
                request.market() + "_" + request.candleType(), candles);

        StrategyParam param = request.resolvedParam();
        Strategy strategy = strategyFactory.build(request.strategyType(), series, param);

        BarSeriesManager manager = new BarSeriesManager(
                series,
                new LinearTransactionCostModel(UPBIT_FEE_RATE),
                new ZeroCostModel()
        );
        TradingRecord tradingRecord = manager.run(strategy);

        PerformanceMetrics metrics = performanceService.analyze(series, tradingRecord, request.initialCapital());

        BigDecimal finalValue = calculateFinalValue(request.initialCapital(), metrics.getTotalReturn());

        BacktestResult result = BacktestResult.of(
                request.market(),
                request.candleType(),
                request.strategyType(),
                request.startDate(),
                request.endDate(),
                request.initialCapital(),
                finalValue,
                metrics
        );

        saveTrades(result, tradingRecord, series);
        BacktestResult saved = resultRepository.save(result);

        return BacktestResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public BacktestResponse findById(Long id) {
        return resultRepository.findById(id)
                .map(BacktestResponse::from)
                .orElseThrow(() -> new UpException(ErrorCode.BACKTEST_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<BacktestResponse> findHistory(
            String market,
            CandleType candleType,
            StrategyType strategyType) {
        List<BacktestResult> results;
        if (candleType != null && strategyType != null) {
            results = resultRepository
                    .findByMarketAndCandleTypeAndStrategyTypeOrderByCreatedAtDesc(
                            market, candleType, strategyType);
        } else {
            results = resultRepository.findByMarketOrderByCreatedAtDesc(market);
        }
        return results.stream().map(BacktestResponse::from).toList();
    }

    @Transactional
    public List<BacktestResponse> runCompare(List<BacktestRequest> requests) {
        return requests.stream()
                .map(this::run)
                .toList();
    }

    private void saveTrades(BacktestResult result, TradingRecord tradingRecord, BarSeries series) {
        for (Position position : tradingRecord.getPositions()) {
            if (!position.isClosed()) {
                continue;
            }

            org.ta4j.core.Trade entry = position.getEntry();
            org.ta4j.core.Trade exit = position.getExit();

            LocalDateTime entryTime = series.getBar(entry.getIndex())
                    .getEndTime().toLocalDateTime();
            LocalDateTime exitTime = series.getBar(exit.getIndex())
                    .getEndTime().toLocalDateTime();

            BigDecimal entryPrice = BigDecimal.valueOf(entry.getNetPrice().doubleValue());
            BigDecimal exitPrice = BigDecimal.valueOf(exit.getNetPrice().doubleValue());
            BigDecimal quantity = BigDecimal.valueOf(entry.getAmount().doubleValue());

            result.addTrade(Trade.of(result, TradeType.BUY, entryTime, entryPrice, quantity));
            result.addTrade(Trade.of(result, TradeType.SELL, exitTime, exitPrice, quantity));
        }
    }

    private BigDecimal calculateFinalValue(BigDecimal initialCapital, double totalReturn) {
        return initialCapital.multiply(BigDecimal.valueOf(1.0 + totalReturn));
    }
}
