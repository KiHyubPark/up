---
name: candle-collect
description: |
  업비트 캔들 데이터 수집 패턴 가이드. 동기/비동기 수집, 429 재시도, 배치 트랜잭션 분리, 레이트 리미터 설정을 다룬다.
  새 캔들 타입 수집 추가, 대량 수집 로직 수정, 429 에러 처리 변경, 비동기 수집 엔드포인트 추가 시 이 스킬을 사용한다.
---

# 캔들 수집 패턴

업비트에서 캔들 데이터를 배치(200개)로 수집하고 H2 DB에 저장하는 패턴.
대량 수집 시 429 재시도와 비동기 처리를 함께 사용한다.

## 핵심 클래스 위치

```
com.clone.up.domain.candle.service.CandleCollectService   # 수집 오케스트레이터
com.clone.up.domain.candle.service.CandleSaveService      # 배치 저장 (트랜잭션 분리)
com.clone.up.domain.candle.controller.CandleController    # REST 엔드포인트
com.clone.up.config.AsyncConfig                           # 비동기 스레드풀
com.clone.up.config.FeignConfig                           # 레이트 리미터
```

## 수집 흐름

```
Controller → CandleCollectService.collectXxxCandles()
               └─ while (saved < totalCount)
                    └─ fetchWithRetry()          ← 429 시 재시도
                         └─ upbitApiClient.get…() (200개씩)
                    └─ CandleSaveService.saveBatch()  ← 별도 트랜잭션
```

배치마다 트랜잭션을 분리(`CandleSaveService`)하는 이유: 429로 수집이 중단돼도 이미 저장된 배치는 롤백되지 않는다.

## CandleCollectService 핵심 구조

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class CandleCollectService {

    private static final int MAX_COUNT_PER_REQUEST = 200;
    private static final int MAX_RETRY = 3;
    private static final long RETRY_DELAY_MS = 2000; // 429 첫 재시도 대기 (ms)

    private final UpbitApiClient upbitApiClient;
    private final CandleSaveService candleSaveService;

    public int collectMinuteCandles(String market, int unit, int totalCount) {
        CandleType candleType = minuteUnitToCandleType(unit);
        int saved = 0;
        String to = null;

        while (saved < totalCount) {
            int count = Math.min(MAX_COUNT_PER_REQUEST, totalCount - saved);
            final String currentTo = to;  // 람다 캡처용 effectively-final 복사본
            List<UpbitCandleResponse> responses = fetchWithRetry(
                    () -> upbitApiClient.getMinuteCandles(unit, market, currentTo, count),
                    "분 캔들", market
            );
            if (responses.isEmpty()) break;

            saved += candleSaveService.saveBatch(responses, candleType);
            to = responses.getLast().candleDateTimeUtc(); // 다음 배치의 to 파라미터
        }
        return saved;
    }
}
```

**주의**: `to`는 루프에서 재할당되므로 람다 안에서 직접 캡처 불가. 반드시 `final String currentTo = to`로 복사 후 사용.

## 429 재시도 패턴 (fetchWithRetry)

```java
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
            long delay = RETRY_DELAY_MS * attempt; // 지수 백오프: 2s, 4s, 6s
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
```

업비트 429 발생 케이스:
- 초당 10회 초과 → 약 1초 대기면 충분
- 분당 600회 초과 → 최대 60초 대기 필요

대량 수집(52,000개+)처럼 분당 초과가 우려되면 `RETRY_DELAY_MS`를 10,000(10초)으로 높인다.

## 비동기 수집

```java
// CandleCollectService
@Async("candleCollectExecutor")
public void collectMinuteCandlesAsync(String market, int unit, int totalCount) {
    log.info("비동기 분 캔들 수집 시작: market={}, unit={}, totalCount={}", market, unit, totalCount);
    collectMinuteCandles(market, unit, totalCount);
}
```

```java
// CandleController
@ResponseStatus(HttpStatus.ACCEPTED)
@PostMapping("/minutes/async")
public ApiResponse<String> collectMinuteCandlesAsync(
        @RequestParam(defaultValue = "KRW-BTC") String market,
        @RequestParam(defaultValue = "5") int unit,
        @RequestParam(defaultValue = "52000") int count
) {
    candleCollectService.collectMinuteCandlesAsync(market, unit, count);
    return ApiResponse.ok("수집이 시작되었습니다. market=%s, unit=%d, count=%d".formatted(market, unit, count));
}
```

- 컨트롤러는 즉시 **202 Accepted** 반환
- 실제 수집은 `candleCollectExecutor` 스레드에서 백그라운드 진행
- 진행 상황은 서버 로그로만 확인 가능

## AsyncConfig (스레드풀)

```java
@EnableAsync
@Configuration
public class AsyncConfig {

    @Bean(name = "candleCollectExecutor")
    public Executor candleCollectExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);       // 동시 수집 1개 제한 (레이트 리밋 충돌 방지)
        executor.setQueueCapacity(5);     // 대기 큐 최대 5개
        executor.setThreadNamePrefix("candle-collect-");
        executor.initialize();
        return executor;
    }
}
```

동시 수집을 1개로 제한하는 이유: 업비트 레이트 리밋(초당 10회)과 충돌 방지.

## FeignConfig 레이트 리미터

```java
@Bean
public RequestInterceptor rateLimitInterceptor() {
    RateLimiter limiter = RateLimiter.create(4.0); // 초당 4회 (버스트 포함해도 안전)
    return template -> limiter.acquire();
}
```

Guava `RateLimiter`는 버스트(미사용 토큰 누적)를 허용하므로 실제 초당 요청이 설정값보다 높아질 수 있다.
대량 수집 시 429가 잦으면 값을 낮춘다: `4.0` → `2.0`.

## CandleSaveService (트랜잭션 분리)

```java
@Service
@RequiredArgsConstructor
public class CandleSaveService {

    @Transactional
    public int saveBatch(List<UpbitCandleResponse> responses, CandleType candleType) {
        int count = 0;
        for (UpbitCandleResponse r : responses) {
            LocalDateTime candleTime = LocalDateTime.parse(r.candleDateTimeUtc(), UTC_FORMATTER);
            // 중복 저장 방지
            if (candleRepository.existsByMarketAndCandleTypeAndCandleTime(
                    r.market(), candleType, candleTime)) {
                continue;
            }
            candleRepository.save(Candle.of(...));
            count++;
        }
        return count;
    }
}
```

`CandleCollectService`와 같은 클래스에 두면 Spring 프록시를 우회해 `@Transactional`이 동작하지 않는다. 반드시 별도 빈으로 분리.

## 지원 CandleType

```java
public enum CandleType {
    MINUTE_1, MINUTE_5, MINUTE_15, MINUTE_60, MINUTE_240,
    DAY, WEEK
}
```

분봉 unit → CandleType 변환:

| unit 파라미터 | CandleType |
|---|---|
| 1 | MINUTE_1 |
| 5 | MINUTE_5 |
| 15 | MINUTE_15 |
| 60 | MINUTE_60 |
| 240 | MINUTE_240 |

그 외 값은 `INVALID_INPUT` 예외.

## REST 엔드포인트 목록

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/api/v1/candles/minutes` | 분봉 동기 수집 (즉시 결과 반환) |
| POST | `/api/v1/candles/minutes/async` | 분봉 비동기 수집 (202 즉시 반환) |
| POST | `/api/v1/candles/days` | 일봉 동기 수집 |
| POST | `/api/v1/candles/weeks` | 주봉 동기 수집 |

## 새 캔들 타입 수집 추가 체크리스트

- [ ] `CandleType`에 새 타입 추가
- [ ] `UpbitApiClient`에 해당 API 메서드 추가
- [ ] `CandleCollectService`에 `collectXxxCandles()` 추가 (`fetchWithRetry` 재사용)
- [ ] 필요 시 `collectXxxCandlesAsync()` 추가
- [ ] `CandleController`에 엔드포인트 추가
- [ ] `AsyncConfig` 스레드풀 용량 조정 여부 검토
