package com.clone.up.domain.candle.repository;

import com.clone.up.domain.candle.entity.Candle;
import com.clone.up.domain.candle.entity.CandleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CandleRepository extends JpaRepository<Candle, Long> {

    List<Candle> findByMarketAndCandleTypeAndCandleTimeBetweenOrderByCandleTimeAsc(
            String market, CandleType candleType, LocalDateTime from, LocalDateTime to);

    Optional<Candle> findTopByMarketAndCandleTypeOrderByCandleTimeDesc(
            String market, CandleType candleType);

    boolean existsByMarketAndCandleTypeAndCandleTime(
            String market, CandleType candleType, LocalDateTime candleTime);
}
