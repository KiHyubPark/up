package com.clone.up.domain.candle.service;

import com.clone.up.client.UpbitApiClient;
import com.clone.up.domain.candle.dto.UpbitCandleResponse;
import com.clone.up.domain.candle.entity.CandleType;
import com.clone.up.global.exception.ErrorCode;
import com.clone.up.global.exception.UpException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandleCollectService {

    private static final int MAX_COUNT_PER_REQUEST = 200;

    private final UpbitApiClient upbitApiClient;
    private final CandleSaveService candleSaveService;

    /**
     * KRW-BTC 5분봉 자동 수집 (5분마다)
     */
    @Scheduled(cron = "0 */5 * * * *")
    public void scheduledCollect() {
        log.info("KRW-BTC 5분봉 자동 수집 시작");
        collectMinuteCandles("KRW-BTC", 5, MAX_COUNT_PER_REQUEST);
    }

    /**
     * 분 캔들 수집 — 배치(200개)마다 별도 트랜잭션으로 커밋하여 429 발생 시 이전 저장분 보존
     */
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
                log.error("분 캔들 수집 실패: market={}, unit={}, saved={}", market, unit, saved, e);
                throw new UpException(ErrorCode.CANDLE_COLLECT_FAILED);
            }
            if (responses.isEmpty()) break;

            saved += candleSaveService.saveBatch(responses, candleType);
            to = responses.getLast().candleDateTimeUtc();
        }
        log.info("분 캔들 수집 완료: market={}, unit={}, saved={}", market, unit, saved);
        return saved;
    }

    /**
     * 일 캔들 수집 — 배치마다 별도 트랜잭션
     */
    public int collectDayCandles(String market, int totalCount) {
        int saved = 0;
        String to = null;

        while (saved < totalCount) {
            int count = Math.min(MAX_COUNT_PER_REQUEST, totalCount - saved);
            List<UpbitCandleResponse> responses;
            try {
                responses = upbitApiClient.getDayCandles(market, to, count);
            } catch (Exception e) {
                log.error("일 캔들 수집 실패: market={}, saved={}", market, saved, e);
                throw new UpException(ErrorCode.CANDLE_COLLECT_FAILED);
            }
            if (responses.isEmpty()) break;

            saved += candleSaveService.saveBatch(responses, CandleType.DAY);
            to = responses.getLast().candleDateTimeUtc();
        }
        log.info("일 캔들 수집 완료: market={}, saved={}", market, saved);
        return saved;
    }

    /**
     * 주 캔들 수집 — 배치마다 별도 트랜잭션
     */
    public int collectWeekCandles(String market, int totalCount) {
        int saved = 0;
        String to = null;

        while (saved < totalCount) {
            int count = Math.min(MAX_COUNT_PER_REQUEST, totalCount - saved);
            List<UpbitCandleResponse> responses;
            try {
                responses = upbitApiClient.getWeekCandles(market, to, count);
            } catch (Exception e) {
                log.error("주 캔들 수집 실패: market={}, saved={}", market, saved, e);
                throw new UpException(ErrorCode.CANDLE_COLLECT_FAILED);
            }
            if (responses.isEmpty()) break;

            saved += candleSaveService.saveBatch(responses, CandleType.WEEK);
            to = responses.getLast().candleDateTimeUtc();
        }
        log.info("주 캔들 수집 완료: market={}, saved={}", market, saved);
        return saved;
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
