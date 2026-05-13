package com.clone.up.domain.candle.service;

import com.clone.up.client.UpbitApiClient;
import com.clone.up.domain.candle.dto.UpbitCandleResponse;
import com.clone.up.domain.candle.entity.CandleType;
import com.clone.up.global.exception.ErrorCode;
import com.clone.up.global.exception.UpException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
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

    private static final int MAX_RETRY = 3;
    private static final long RETRY_DELAY_MS = 2000;

    /**
     * 분 캔들 수집 — 배치(200개)마다 별도 트랜잭션으로 커밋하여 429 발생 시 이전 저장분 보존
     */
    public int collectMinuteCandles(String market, int unit, int totalCount) {
        CandleType candleType = minuteUnitToCandleType(unit);
        int saved = 0;
        String to = null;

        while (saved < totalCount) {
            int count = Math.min(MAX_COUNT_PER_REQUEST, totalCount - saved);
            final String currentTo = to;
            List<UpbitCandleResponse> responses = fetchWithRetry(
                    () -> upbitApiClient.getMinuteCandles(unit, market, currentTo, count),
                    "분 캔들", market
            );
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
            final String currentTo = to;
            List<UpbitCandleResponse> responses = fetchWithRetry(
                    () -> upbitApiClient.getDayCandles(market, currentTo, count),
                    "일 캔들", market
            );
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
            final String currentTo = to;
            List<UpbitCandleResponse> responses = fetchWithRetry(
                    () -> upbitApiClient.getWeekCandles(market, currentTo, count),
                    "주 캔들", market
            );
            if (responses.isEmpty()) break;

            saved += candleSaveService.saveBatch(responses, CandleType.WEEK);
            to = responses.getLast().candleDateTimeUtc();
        }
        log.info("주 캔들 수집 완료: market={}, saved={}", market, saved);
        return saved;
    }

    /**
     * 429 발생 시 최대 MAX_RETRY회 재시도 (지수 백오프)
     */
    private List<UpbitCandleResponse> fetchWithRetry(
            ApiCall<List<UpbitCandleResponse>> call, String label, String market) {
        int attempt = 0;
        while (true) {
            try {
                return call.execute();
            } catch (FeignException.TooManyRequests e) {
                attempt++;
                if (attempt > MAX_RETRY) {
                    log.error("{} 수집 실패: market={}, 재시도 초과", label, market);
                    throw new UpException(ErrorCode.CANDLE_COLLECT_FAILED);
                }
                long delay = RETRY_DELAY_MS * attempt;
                log.warn("{} 429 Too Many Requests — {}ms 후 재시도 ({}/{}): market={}",
                        label, delay, attempt, MAX_RETRY, market);
                sleep(delay);
            } catch (Exception e) {
                log.error("{} 수집 실패: market={}", label, market, e);
                throw new UpException(ErrorCode.CANDLE_COLLECT_FAILED);
            }
        }
    }

    @FunctionalInterface
    private interface ApiCall<T> {
        T execute() throws Exception;
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    @Async("candleCollectExecutor")
    public void collectMinuteCandlesAsync(String market, int unit, int totalCount) {
        log.info("비동기 분 캔들 수집 시작: market={}, unit={}, totalCount={}", market, unit, totalCount);
        collectMinuteCandles(market, unit, totalCount);
    }

    @Async("candleCollectExecutor")
    public void collectDayCandlesAsync(String market, int totalCount) {
        log.info("비동기 일 캔들 수집 시작: market={}, totalCount={}", market, totalCount);
        collectDayCandles(market, totalCount);
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
