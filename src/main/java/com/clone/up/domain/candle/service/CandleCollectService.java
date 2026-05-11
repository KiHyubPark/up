package com.clone.up.domain.candle.service;

import com.clone.up.client.UpbitApiClient;
import com.clone.up.domain.candle.dto.UpbitCandleResponse;
import com.clone.up.domain.candle.entity.Candle;
import com.clone.up.domain.candle.entity.CandleType;
import com.clone.up.domain.candle.repository.CandleRepository;
import com.clone.up.global.exception.ErrorCode;
import com.clone.up.global.exception.UpException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandleCollectService {

    private static final int MAX_COUNT_PER_REQUEST = 200;
    private static final DateTimeFormatter UTC_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final UpbitApiClient upbitApiClient;
    private final CandleRepository candleRepository;

    /**
     * KRW-BTC 5분봉 자동 수집 (5분마다)
     */
    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void scheduledCollect() {
        log.info("KRW-BTC 5분봉 자동 수집 시작");
        collectMinuteCandles("KRW-BTC", 5, MAX_COUNT_PER_REQUEST);
    }

    /**
     * 분 캔들 수집
     *
     * @param market     마켓 코드 (예: KRW-BTC)
     * @param unit       분 단위 (1, 5, 15, 60, 240)
     * @param totalCount 수집할 총 캔들 수
     */
    @Transactional
    public int collectMinuteCandles(String market, int unit, int totalCount) {
        CandleType candleType = minuteUnitToCandleType(unit);
        int saved = 0;
        String to = null;

        while (saved < totalCount) {
            int count = Math.min(MAX_COUNT_PER_REQUEST, totalCount - saved);
            List<UpbitCandleResponse> responses;
            try {
                responses = upbitApiClient.getMinuteCandles(unit, market, to, count);
            } catch (Exception e) {
                log.error("분 캔들 수집 실패: market={}, unit={}", market, unit, e);
                throw new UpException(ErrorCode.CANDLE_COLLECT_FAILED);
            }
            if (responses.isEmpty()) break;

            saved += saveCandles(responses, candleType);
            to = responses.get(responses.size() - 1).candleDateTimeUtc();
        }
        log.info("분 캔들 수집 완료: market={}, unit={}, saved={}", market, unit, saved);
        return saved;
    }

    /**
     * 일 캔들 수집 (페이징 지원)
     *
     * @param market     마켓 코드
     * @param totalCount 수집할 총 캔들 수
     */
    @Transactional
    public int collectDayCandles(String market, int totalCount) {
        int saved = 0;
        String to = null;

        while (saved < totalCount) {
            int count = Math.min(MAX_COUNT_PER_REQUEST, totalCount - saved);
            List<UpbitCandleResponse> responses;
            try {
                responses = upbitApiClient.getDayCandles(market, to, count);
            } catch (Exception e) {
                log.error("일 캔들 수집 실패: market={}", market, e);
                throw new UpException(ErrorCode.CANDLE_COLLECT_FAILED);
            }
            if (responses.isEmpty()) break;

            saved += saveCandles(responses, CandleType.DAY);
            to = responses.get(responses.size() - 1).candleDateTimeUtc();
        }
        log.info("일 캔들 수집 완료: market={}, saved={}", market, saved);
        return saved;
    }

    /**
     * 주 캔들 수집 (페이징 지원)
     *
     * @param market     마켓 코드
     * @param totalCount 수집할 총 캔들 수
     */
    @Transactional
    public int collectWeekCandles(String market, int totalCount) {
        int saved = 0;
        String to = null;

        while (saved < totalCount) {
            int count = Math.min(MAX_COUNT_PER_REQUEST, totalCount - saved);
            List<UpbitCandleResponse> responses;
            try {
                responses = upbitApiClient.getWeekCandles(market, to, count);
            } catch (Exception e) {
                log.error("주 캔들 수집 실패: market={}", market, e);
                throw new UpException(ErrorCode.CANDLE_COLLECT_FAILED);
            }
            if (responses.isEmpty()) break;

            saved += saveCandles(responses, CandleType.WEEK);
            to = responses.get(responses.size() - 1).candleDateTimeUtc();
        }
        log.info("주 캔들 수집 완료: market={}, saved={}", market, saved);
        return saved;
    }

    private int saveCandles(List<UpbitCandleResponse> responses, CandleType candleType) {
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

    private CandleType minuteUnitToCandleType(int unit) {
        return switch (unit) {
            case 1 -> CandleType.MINUTE_1;
            case 5 -> CandleType.MINUTE_5;
            case 15 -> CandleType.MINUTE_15;
            case 60 -> CandleType.MINUTE_60;
            case 240 -> CandleType.MINUTE_240;
            default -> throw new UpException(ErrorCode.INVALID_INPUT);
        };
    }
}
