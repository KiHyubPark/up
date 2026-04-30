---
name: upbit-feign-client
description: |
  업비트 API 연동 패턴 가이드. UpbitApiClient(Feign)에 새 엔드포인트 추가, UpbitErrorDecoder 에러 처리, FeignConfig 설정을 다룬다.
  새 업비트 API를 호출해야 할 때, Feign 클라이언트 메서드 추가가 필요할 때, Rate Limit/에러 처리 방법이 필요할 때 이 스킬을 사용한다.
---

# 업비트 Feign 클라이언트 패턴

업비트 REST API는 `UpbitApiClient` (OpenFeign 인터페이스)를 통해 호출한다.
에러 처리는 `UpbitErrorDecoder`가 담당하고, 타임아웃·로그 설정은 `FeignConfig`에 있다.

## 핵심 클래스 위치

```
com.clone.up.client.UpbitApiClient       # Feign 클라이언트 인터페이스
com.clone.up.config.UpbitErrorDecoder    # HTTP 에러 → UpException 변환
com.clone.up.config.FeignConfig          # 타임아웃, 로그 레벨 설정
```

## UpbitApiClient 구조

```java
@FeignClient(name = "upbit", url = "${feign.upbit.base-url}")
public interface UpbitApiClient {

    @RequestMapping(method = RequestMethod.GET, value = "/v1/ticker")
    List<TickerResponse> getTicker(@RequestParam String markets);

    @RequestMapping(method = RequestMethod.GET, value = "/v1/market/all")
    List<PairResponse> getPairs();
}
```

Base URL은 `application.yml`의 `feign.upbit.base-url`에서 읽는다.

## 새 업비트 API 엔드포인트 추가

### 1단계: 응답 DTO 정의

업비트 API 응답 필드를 `record`로 정의한다. `domain/market/dto/` 하위에 둔다.

```java
// 예: 호가(Orderbook) 응답
public record OrderbookResponse(
        String market,
        Long timestamp,
        Double totalAskSize,
        Double totalBidSize,
        List<OrderbookUnit> orderbookUnits
) {
    public record OrderbookUnit(
            Double askPrice,
            Double bidPrice,
            Double askSize,
            Double bidSize
    ) {}
}
```

필드명은 업비트 API 응답의 snake_case 키를 camelCase로 변환한다. Spring은 기본적으로 `snake_case → camelCase` Jackson 매핑을 지원한다. 불일치가 있으면 `@JsonProperty`를 사용한다.

```java
public record SomeResponse(
        @JsonProperty("trade_date_utc") String tradeDateUtc
) {}
```

### 2단계: UpbitApiClient에 메서드 추가

```java
@FeignClient(name = "upbit", url = "${feign.upbit.base-url}")
public interface UpbitApiClient {

    // 기존 메서드...

    // 새 엔드포인트 추가
    @RequestMapping(method = RequestMethod.GET, value = "/v1/orderbook")
    List<OrderbookResponse> getOrderbook(@RequestParam String markets);
}
```

파라미터가 여러 개면 각각 `@RequestParam`으로 선언한다.

```java
@RequestMapping(method = RequestMethod.GET, value = "/v1/candles/minutes/{unit}")
List<CandleResponse> getMinuteCandles(
        @PathVariable int unit,
        @RequestParam String market,
        @RequestParam(required = false) Integer count
);
```

### 3단계: 서비스에서 호출

```java
@Service
@RequiredArgsConstructor
public class MarketService {

    private final UpbitApiClient upbitApiClient;

    public List<OrderbookResponse> getOrderbook(String markets) {
        return upbitApiClient.getOrderbook(markets);
    }
}
```

Feign 호출 중 에러가 발생하면 `UpbitErrorDecoder`가 `UpException`으로 변환한다. 서비스에서 따로 `try/catch` 하지 않아도 된다.

## UpbitErrorDecoder 동작

```java
public class UpbitErrorDecoder implements ErrorDecoder {
    @Override
    public Exception decode(String methodKey, Response response) {
        return switch (response.status()) {
            case 400 -> new UpException(ErrorCode.UPBIT_INVALID_MARKET);
            case 429 -> new UpException(ErrorCode.UPBIT_RATE_LIMIT);
            default  -> new UpException(ErrorCode.UPBIT_API_ERROR);
        };
    }
}
```

새 에러 케이스가 필요하면 `ErrorCode`에 상수를 추가하고 `switch`에 케이스를 추가한다.

## FeignConfig 설정

```java
@Configuration
public class FeignConfig {

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;  // 요청/응답 요약만 로그
    }

    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(5_000, 10_000);  // connectTimeout=5s, readTimeout=10s
    }
}
```

로그 레벨 옵션: `NONE` → `BASIC` → `HEADERS` → `FULL`  
개발 환경에서 상세 디버깅이 필요하면 `FULL`로 임시 변경한다.

## application.yml 설정

```yaml
feign:
  upbit:
    base-url: https://api.upbit.com

logging:
  level:
    com.clone.up.client: DEBUG  # Feign 로그 출력 (BASIC 이상일 때)
```

## Rate Limit 대응

업비트는 초당 요청 수 제한이 있다 (공개 API: 초당 10회).
`429 Too Many Requests` 응답 시 `UpbitErrorDecoder`가 `UPBIT_RATE_LIMIT` 에러로 변환한다.

백테스팅 등 대량 호출 시나리오에서는 서비스 계층에서 요청 간 딜레이를 추가한다.

```java
// 예: 대량 캔들 조회 시
for (String market : markets) {
    candles.addAll(upbitApiClient.getMinuteCandles(1, market, 200));
    Thread.sleep(110); // 초당 9회 이하 유지
}
```

## 체크리스트

새 업비트 API 추가 시:
- [ ] 응답 DTO `record` 작성 (`domain/market/dto/`)
- [ ] `UpbitApiClient`에 메서드 추가
- [ ] 서비스 메서드 추가 (예외 처리 불필요)
- [ ] 컨트롤러에서 `ApiResponse.ok(...)` 반환
- [ ] 필요 시 `ErrorCode`에 에러 상수 추가
