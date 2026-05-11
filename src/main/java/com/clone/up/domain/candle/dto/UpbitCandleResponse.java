package com.clone.up.domain.candle.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record UpbitCandleResponse(
        String market,
        @JsonProperty("candle_date_time_utc") String candleDateTimeUtc,
        @JsonProperty("opening_price") BigDecimal openingPrice,
        @JsonProperty("high_price") BigDecimal highPrice,
        @JsonProperty("low_price") BigDecimal lowPrice,
        @JsonProperty("trade_price") BigDecimal tradePrice,
        @JsonProperty("candle_acc_trade_volume") BigDecimal candleAccTradeVolume,
        @JsonProperty("candle_acc_trade_price") BigDecimal candleAccTradePrice
) {
}
