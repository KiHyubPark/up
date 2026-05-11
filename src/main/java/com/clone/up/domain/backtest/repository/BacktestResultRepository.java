package com.clone.up.domain.backtest.repository;

import com.clone.up.domain.backtest.entity.BacktestResult;
import com.clone.up.domain.candle.entity.CandleType;
import com.clone.up.domain.strategy.StrategyType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BacktestResultRepository extends JpaRepository<BacktestResult, Long> {

    List<BacktestResult> findByMarketAndCandleTypeAndStrategyTypeOrderByCreatedAtDesc(
            String market, CandleType candleType, StrategyType strategyType);

    List<BacktestResult> findByMarketOrderByCreatedAtDesc(String market);
}
