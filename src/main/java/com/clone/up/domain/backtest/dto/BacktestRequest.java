package com.clone.up.domain.backtest.dto;

import com.clone.up.domain.candle.entity.CandleType;
import com.clone.up.domain.strategy.StrategyParam;
import com.clone.up.domain.strategy.StrategyType;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BacktestRequest(
        @JsonProperty("market") @NotBlank String market,
        @JsonProperty("candleType") @NotNull CandleType candleType,
        @JsonProperty("startDate") @NotNull LocalDateTime startDate,
        @JsonProperty("endDate") @NotNull LocalDateTime endDate,
        @JsonProperty("strategyType") @NotNull StrategyType strategyType,
        @JsonProperty("param") StrategyParam param,
        @JsonProperty("initialCapital") @NotNull @Positive BigDecimal initialCapital
) {
    public StrategyParam resolvedParam() {
        return param != null ? param : StrategyParam.defaults();
    }
}
