package com.clone.up.domain.candle.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "candle",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_candle_market_type_time",
        columnNames = {"market", "candle_type", "candle_time"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Candle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String market;

    @Enumerated(EnumType.STRING)
    @Column(name = "candle_type", nullable = false, length = 20)
    private CandleType candleType;

    @Column(name = "candle_time", nullable = false)
    private LocalDateTime candleTime;

    @Column(nullable = false, precision = 30, scale = 8)
    private BigDecimal openPrice;

    @Column(nullable = false, precision = 30, scale = 8)
    private BigDecimal highPrice;

    @Column(nullable = false, precision = 30, scale = 8)
    private BigDecimal lowPrice;

    @Column(nullable = false, precision = 30, scale = 8)
    private BigDecimal closePrice;

    @Column(nullable = false, precision = 30, scale = 8)
    private BigDecimal volume;

    @Column(nullable = false, precision = 30, scale = 8)
    private BigDecimal tradedValue;

    public static Candle of(
            String market,
            CandleType candleType,
            LocalDateTime candleTime,
            BigDecimal openPrice,
            BigDecimal highPrice,
            BigDecimal lowPrice,
            BigDecimal closePrice,
            BigDecimal volume,
            BigDecimal tradedValue
    ) {
        Candle candle = new Candle();
        candle.market = market;
        candle.candleType = candleType;
        candle.candleTime = candleTime;
        candle.openPrice = openPrice;
        candle.highPrice = highPrice;
        candle.lowPrice = lowPrice;
        candle.closePrice = closePrice;
        candle.volume = volume;
        candle.tradedValue = tradedValue;
        return candle;
    }
}
