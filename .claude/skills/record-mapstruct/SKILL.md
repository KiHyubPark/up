---
name: record-mapstruct
description: Java record를 Request/Response DTO로 사용하고, MapStruct로 엔터티 ↔ DTO 변환을 구현하는 패턴. Spring Boot 프로젝트에서 DTO 클래스 작성, API 컨트롤러/서비스 레이어 구현, 매퍼 인터페이스 생성 시 반드시 이 스킬을 사용할 것. 새로운 도메인 추가, 엔드포인트 추가, 엔터티 수정 작업에도 적용.
---

# Record + MapStruct 패턴

Java `record`를 DTO로 사용하고 MapStruct로 변환하는 Spring Boot 패턴.

## 의존성 설정

### Gradle (build.gradle)

```groovy
dependencies {
    implementation 'org.mapstruct:mapstruct:1.5.5.Final'

    // Lombok 사용 시 — 순서가 중요하다
    annotationProcessor 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok-mapstruct-binding:0.2.0'
    annotationProcessor 'org.mapstruct:mapstruct-processor:1.5.5.Final'

    // Lombok 미사용 시
    // annotationProcessor 'org.mapstruct:mapstruct-processor:1.5.5.Final'
}
```

> Lombok과 함께 쓸 때 `lombok` → `lombok-mapstruct-binding` → `mapstruct-processor` 순서를 반드시 지킨다. 순서가 틀리면 MapStruct가 Lombok이 생성한 getter를 인식하지 못해 컴파일 오류가 난다.

---

## Record DTO 작성 규칙

### Request Record

검증 어노테이션은 record의 compact constructor 또는 컴포넌트 파라미터에 직접 붙인다.

```java
public record CreateCandleRequest(
    @NotBlank String market,
    @NotNull LocalDateTime candleTime,
    @NotNull BigDecimal openPrice,
    @NotNull BigDecimal highPrice,
    @NotNull BigDecimal lowPrice,
    @NotNull BigDecimal closePrice,
    @NotNull BigDecimal volume
) {}
```

컨트롤러에서는 `@RequestBody @Valid`로 받는다.

```java
@PostMapping
public ResponseEntity<ApiResponse<CandleResponse>> create(
        @RequestBody @Valid CreateCandleRequest request) {
    return ResponseEntity.ok(ApiResponse.success(candleService.create(request)));
}
```

### Response Record

엔터티의 민감한 필드는 노출하지 않도록 필요한 필드만 선언한다.

```java
public record CandleResponse(
    Long id,
    String market,
    LocalDateTime candleTime,
    BigDecimal openPrice,
    BigDecimal highPrice,
    BigDecimal lowPrice,
    BigDecimal closePrice,
    BigDecimal volume
) {}
```

### 페이지 응답

```java
public record CandlePageResponse(
    List<CandleResponse> content,
    long totalElements,
    int totalPages,
    int page,
    int size
) {
    public static CandlePageResponse from(Page<CandleResponse> page) {
        return new CandlePageResponse(
            page.getContent(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.getNumber(),
            page.getSize()
        );
    }
}
```

---

## MapStruct 매퍼 작성

### 기본 매퍼

```java
@Mapper(componentModel = "spring")
public interface CandleMapper {

    // Request record → Entity
    Candle toEntity(CreateCandleRequest request);

    // Entity → Response record
    CandleResponse toResponse(Candle candle);

    // Entity 리스트 → Response 리스트
    List<CandleResponse> toResponseList(List<Candle> candles);
}
```

`componentModel = "spring"`으로 지정하면 Spring Bean으로 등록되어 `@Autowired` / 생성자 주입으로 사용 가능하다.

### 필드 이름이 다를 때 (@Mapping)

```java
@Mapper(componentModel = "spring")
public interface TradeMapper {

    @Mapping(source = "tradeType", target = "type")
    @Mapping(source = "tradeTime", target = "executedAt")
    TradeResponse toResponse(Trade trade);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Trade toEntity(CreateTradeRequest request);
}
```

### 중첩 객체 매핑

```java
@Mapper(componentModel = "spring", uses = {TradeMapper.class})
public interface BacktestResultMapper {

    @Mapping(source = "trades", target = "tradeHistory")
    BacktestResultResponse toResponse(BacktestResult result);
}
```

### 커스텀 변환 로직

단순한 `@Mapping`으로 해결되지 않는 경우 `default` 메서드를 추가한다.

```java
@Mapper(componentModel = "spring")
public interface CandleMapper {

    CandleResponse toResponse(Candle candle);

    default String formatMarket(String market) {
        return market == null ? null : market.toUpperCase();
    }
}
```

---

## 서비스 레이어에서 사용

```java
@Service
@RequiredArgsConstructor
public class CandleService {

    private final CandleRepository candleRepository;
    private final CandleMapper candleMapper;

    public CandleResponse create(CreateCandleRequest request) {
        Candle candle = candleMapper.toEntity(request);
        return candleMapper.toResponse(candleRepository.save(candle));
    }

    public List<CandleResponse> findAll(String market) {
        return candleMapper.toResponseList(
            candleRepository.findByMarket(market)
        );
    }
}
```

---

## 패키지 구조 예시

```
domain/candle/
├── controller/CandleController.java
├── service/CandleService.java
├── repository/CandleRepository.java
├── entity/Candle.java
├── dto/
│   ├── CreateCandleRequest.java   ← record
│   └── CandleResponse.java        ← record
└── mapper/
    └── CandleMapper.java           ← MapStruct interface
```

DTO는 `dto/` 패키지, 매퍼는 `mapper/` 패키지로 분리한다.

---

## 주의사항

| 상황 | 처리 방법 |
|------|----------|
| record 필드와 엔터티 필드 이름 다름 | `@Mapping(source=, target=)` 사용 |
| 매핑에서 특정 필드 제외 | `@Mapping(target="fieldName", ignore=true)` |
| null 처리 정책 전역 설정 | `@Mapper(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)` |
| 양방향 매핑 | `@InheritInverseConfiguration` 사용 |
| Lombok + MapStruct | annotationProcessor 순서 반드시 확인 |
