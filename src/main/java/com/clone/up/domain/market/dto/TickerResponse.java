package com.clone.up.domain.market.dto;

public record TickerResponse(
        String market,
        String tradeDate,
        String tradeTime,
        String tradeDateKst,
        String tradeTimeKst,
        Long tradeTimestamp,
        Long openingPrice,
        Long highPrice,
        Long lowPrice,
        Long tradePrice,
        Long prevClosingPrice,
        String change,
        Long changePrice,
        Double changeRate,
        Long signedChangePrice,
        Double signedChangeRate,
        Double tradeVolume,
        Double accTradePrice,
        Double accTradePrice24h,
        Double accTradeVolume,
        Double accTradeVolume24h,
        Long highest52WeekPrice,
        String highest52WeekDate,
        Long lowest52WeekPrice,
        String lowest52WeekDate,
        Long timestamp
) {
}
