package com.clone.up.domain.trading.dto;

import com.clone.up.domain.candle.entity.CandleType;
import com.clone.up.domain.strategy.StrategyType;
import com.clone.up.domain.trading.entity.LivePosition;
import com.clone.up.domain.trading.entity.PositionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LivePositionResponse(
        Long id,
        String market,
        StrategyType strategyType,
        CandleType candleType,
        PositionStatus status,
        BigDecimal entryPrice,
        LocalDateTime entryTime,
        BigDecimal entryAtrValue,
        BigDecimal quantity,
        BigDecimal investedAmount,
        BigDecimal exitPrice,
        LocalDateTime exitTime,
        BigDecimal realizedPnl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static LivePositionResponse from(LivePosition p) {
        return new LivePositionResponse(
                p.getId(),
                p.getMarket(),
                p.getStrategyType(),
                p.getCandleType(),
                p.getStatus(),
                p.getEntryPrice(),
                p.getEntryTime(),
                p.getEntryAtrValue(),
                p.getQuantity(),
                p.getInvestedAmount(),
                p.getExitPrice(),
                p.getExitTime(),
                p.getRealizedPnl(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
