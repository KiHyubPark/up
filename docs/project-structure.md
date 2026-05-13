# 프로젝트 구조

## 기술 스택

| 항목 | 버전 |
|---|---|
| Java | 21 (LTS) |
| Spring Boot | 3.4.5 |
| Spring Cloud | 2024.0.1 |
| 빌드 | Gradle |
| 주요 라이브러리 | Spring Cloud OpenFeign, Lombok, springdoc-openapi 2.8.6, ta4j-core 0.14 |

## 패키지 구조

```
src/main/java/com/clone/up/
│
├── client/
│   └── UpbitApiClient.java              # 업비트 외부 API Feign 클라이언트
│
├── config/
│   ├── AsyncConfig.java                 # candleCollectExecutor (단일 스레드, 레이트 리미터 충돌 방지)
│   ├── FeignConfig.java                 # Feign 타임아웃, Rate Limit Interceptor (8 req/sec)
│   ├── TradingProperties.java           # trading.* YAML 바인딩 (@ConfigurationProperties)
│   └── UpbitErrorDecoder.java           # 업비트 API 에러 디코더
│
├── domain/
│   ├── market/                          # 시세 도메인
│   │   ├── controller/MarketController.java
│   │   ├── service/MarketService.java
│   │   └── dto/
│   │       ├── TickerResponse.java      # record
│   │       └── PairResponse.java        # record
│   │
│   ├── candle/                          # 캔들 데이터 수집 및 조회
│   │   ├── entity/Candle.java
│   │   ├── entity/CandleType.java       # MINUTE_1/5/15/60, DAY, WEEK
│   │   ├── dto/UpbitCandleResponse.java
│   │   ├── repository/CandleRepository.java
│   │   ├── service/CandleCollectService.java   # 분/일/주 캔들 수집 + 5분마다 자동 수집
│   │   ├── service/CandleSaveService.java      # 중복 제거 bulk upsert
│   │   └── controller/CandleController.java
│   │
│   ├── indicator/                       # 기술지표 계산 (TA4J)
│   │   └── service/IndicatorCalculationService.java  # buildBarSeries + SMA/EMA/RSI/MACD/BB
│   │
│   ├── strategy/                        # 매매 전략
│   │   ├── TradingStrategy.java         # 전략 인터페이스 (buildStrategy)
│   │   ├── StrategyType.java            # GOLDEN_CROSS / RSI_OVERSOLD / MACD_CROSS / BOLLINGER_BAND / SCALPING
│   │   ├── StrategyParam.java           # 전략 파라미터 (rsiOversold, stopLossPercent 등)
│   │   ├── StrategyFactory.java         # StrategyType → TradingStrategy 팩토리
│   │   ├── rule/AtrStopLossRule.java    # ATR 기반 손절 규칙
│   │   ├── service/GoldenCrossStrategy.java
│   │   ├── service/RsiOversoldStrategy.java
│   │   ├── service/MacdCrossStrategy.java
│   │   ├── service/BollingerBandStrategy.java
│   │   └── service/ScalpingStrategy.java       # RSI 과매도 + ATR 손절 최적화 전략
│   │
│   ├── backtest/                        # 백테스팅 엔진
│   │   ├── entity/BacktestResult.java
│   │   ├── entity/Trade.java
│   │   ├── entity/TradeType.java        # BUY | SELL
│   │   ├── entity/PerformanceMetrics.java
│   │   ├── dto/BacktestRequest.java
│   │   ├── dto/BacktestResponse.java
│   │   ├── repository/BacktestResultRepository.java
│   │   ├── service/BacktestExecutionService.java      # 수수료 0.05% 반영 시뮬레이션 루프
│   │   ├── service/BuyAndHoldBenchmarkService.java    # 바이앤홀드 벤치마크
│   │   ├── service/PerformanceAnalysisService.java    # 수익률/MDD/샤프지수 계산
│   │   └── controller/BacktestController.java
│   │
│   └── trading/                         # 자동매매 인프라
│       ├── entity/
│       │   ├── LivePosition.java            # 포지션 영속화 (market, strategyType, entryPrice, quantity)
│       │   ├── PositionStatus.java          # OPEN | CLOSED
│       │   ├── SignalLog.java               # 시그널 이력 (BUY_SIGNAL / SELL_SIGNAL)
│       │   ├── SignalType.java
│       │   ├── DailyTradingRecord.java      # 일일 손실 누계, halted 플래그
│       │   └── TradingMode.java             # PAPER(시그널만) | LIVE(포지션 영속화)
│       ├── dto/
│       │   └── LivePositionResponse.java
│       ├── repository/
│       │   ├── LivePositionRepository.java      # findByMarketAndStrategyTypeAndStatusForUpdate (PESSIMISTIC_WRITE)
│       │   ├── SignalLogRepository.java
│       │   └── DailyTradingRecordRepository.java
│       ├── service/
│       │   ├── LivePositionService.java          # openPosition / closePosition
│       │   ├── TradingSignalEvaluator.java       # DB 캔들 → BarSeries → TA4J entry/exit rule 평가
│       │   ├── TradingExecutionService.java      # 자동매매 메인 오케스트레이터
│       │   ├── DailyRiskGuard.java               # 일일 손실 한도 & halted 상태 관리
│       │   ├── EmergencyStopService.java         # AtomicBoolean 긴급 중단 플래그
│       │   └── OrderGuard.java                  # ConcurrentHashMap 중복 주문 방지 (JVM)
│       ├── scheduler/
│       │   └── TradingScheduler.java             # cron "0 1,16,31,46 * * * *" (15분봉 마감 1분 후)
│       └── controller/
│           ├── LivePositionController.java       # GET /api/positions
│           ├── SignalLogController.java          # GET /api/signals
│           └── EmergencyStopController.java      # POST/DELETE/GET /api/trading/emergency-stop
│
├── global/
│   ├── exception/
│   │   ├── ErrorCode.java           # 에러 코드 enum
│   │   ├── UpException.java         # 공통 비즈니스 예외
│   │   └── GlobalExceptionHandler.java
│   └── response/
│       └── ApiResponse.java         # 공통 응답 포맷
│
└── UpApplication.java               # @EnableScheduling @SpringBootApplication
```

## 패키지 역할

| 패키지 | 역할 |
|---|---|
| `client` | Feign 클라이언트 인터페이스 (외부 API 엔드포인트 정의) |
| `config` | Spring Bean 설정 (Feign, Async, TradingProperties) |
| `domain/{name}/controller` | REST API 엔드포인트 |
| `domain/{name}/service` | 비즈니스 로직 |
| `domain/{name}/dto` | 요청/응답 DTO (record 사용) |
| `domain/{name}/entity` | JPA 엔터티 |
| `domain/{name}/repository` | Spring Data JPA 리포지토리 |
| `domain/trading/scheduler` | 자동매매 스케줄러 |
| `global/exception` | 공통 예외 처리 |
| `global/response` | 공통 응답 포맷 |

## URL 규칙

```
/api/v1/{도메인명}   — 버전이 필요한 외부 공개 API (시세, 백테스팅)
/api/{도메인명}      — 내부 관리 API (포지션, 시그널, 긴급 중단)
```

- 버전은 URL 경로로 관리 (`v1`, `v2`)
- 도메인명은 복수형 사용 (`markets`, `candles`, `positions`, `signals`)

## 자동매매 설계 포인트

### PAPER / LIVE 모드 분기

- `TradingProperties.mode` → `TradingMode.isLive()`
- PAPER: SignalLog만 기록, LivePosition 영속화 없음
- LIVE: SignalLog 기록 + LivePosition 영속화 + DailyRiskGuard.recordClose()

### 중복 주문 방지 2-레이어

1. **OrderGuard** (JVM): `ConcurrentHashMap.newKeySet()` — 동일 JVM 내 동시 진입 차단
2. **LivePositionRepository** (DB): `@Lock(PESSIMISTIC_WRITE)` JPQL — 다중 인스턴스 환경 DB 레벨 방어

### 서버 재시작 후 청산 시그널 복구

`TradingSignalEvaluator.isExitSignal()`:
1. `LivePosition.entryTime`으로 BarSeries에서 `entryBarIndex` 탐색
2. `BaseTradingRecord.enter(entryBarIndex)` 재구성
3. `exitRule.isSatisfied(lastIndex, record)` 평가

### 스케줄러 실행 타이밍

cron `"0 1,16,31,46 * * * *"` — 15분봉 마감(정각/15/30/45분) 직후 1분 대기 후 실행하여 업비트 데이터 전파 지연을 흡수한다.

## 새 도메인 추가 방법

1. `domain/{name}/` 패키지 생성
2. `controller/`, `service/`, `dto/`, `entity/`, `repository/` 하위 패키지 추가
3. DTO는 `record`로 작성
4. 외부 API 호출이 필요하면 `client/{Name}ApiClient.java` 추가
5. `application.yaml`에 URL 환경변수 추가
