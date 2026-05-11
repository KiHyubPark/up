package com.clone.up.domain.candle.service;

import com.clone.up.domain.candle.dto.UpbitCandleResponse;
import com.clone.up.domain.candle.entity.Candle;
import com.clone.up.domain.candle.entity.CandleType;
import com.clone.up.domain.candle.repository.CandleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 배치 단위(200개)로 트랜잭션을 분리하여 저장.
 * CandleCollectService와 같은 빈에 두면 Spring 프록시를 우회하므로 별도 서비스로 분리.
 */
@Service
@RequiredArgsConstructor
public class CandleSaveService {

    private static final DateTimeFormatter UTC_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final CandleRepository candleRepository;

    @Transactional
    public int saveBatch(List<UpbitCandleResponse> responses, CandleType candleType) {
        int count = 0;
        for (UpbitCandleResponse r : responses) {
            LocalDateTime candleTime = LocalDateTime.parse(r.candleDateTimeUtc(), UTC_FORMATTER);
            if (candleRepository.existsByMarketAndCandleTypeAndCandleTime(
                    r.market(), candleType, candleTime)) {
                continue;
            }
            candleRepository.save(Candle.of(
                    r.market(),
                    candleType,
                    candleTime,
                    r.openingPrice(),
                    r.highPrice(),
                    r.lowPrice(),
                    r.tradePrice(),
                    r.candleAccTradeVolume(),
                    r.candleAccTradePrice()
            ));
            count++;
        }
        return count;
    }
}
