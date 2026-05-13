# Up

Upbit API를 기반으로 코인 시세 조회, 백테스팅, 자동매매 인프라를 제공하는 Spring Boot REST API 서버

## 프로젝트 목표

업비트(Upbit) 거래소 API를 활용하여 **KRW-BTC 등 코인 쌍에 대한 매매 전략 백테스팅 플랫폼**을 구축한다.

과거 OHLCV(시가·고가·저가·종가·거래량) 캔들 데이터를 수집·저장하고, TA4J 기반 기술지표(EMA, RSI, ATR, ADX, 볼륨)를 계산하여 EMA 스캘핑 전략을 시뮬레이션한다. 시뮬레이션 결과로 수익률·최대낙폭·승률·샤프지수 등 성과지표를 제공하며, 자동매매 인프라(PAPER/LIVE 모드, 포지션 영속화, 일일 손실 한도, 중복 주문 방지, 긴급 중단)까지 구현한다.

### 구현 단계

| Phase | 내용 | 상태 |
|-------|------|------|
| 0 | Upbit 시세 조회 API (ticker, pair) | ✅ 완료 |
| 1 | 캔들 API 추가 + Candle 엔터티 + DB 저장 + 스케줄 수집 | ✅ 완료 |
| 2 | TA4J 통합 + 기술지표 계산 서비스 (SMA / EMA / RSI / MACD / BB) | ✅ 완료 |
| 3 | 매매 전략 구현 (EMA 스캘핑 v2: EMA + RSI + ADX + ATR + 볼륨 + HTF 필터) | ✅ 완료 |
| 4 | 백테스팅 엔진 (시뮬레이션 루프 + 수수료 반영) | ✅ 완료 |
| 5 | 성과지표 계산 + REST API 노출 | ✅ 완료 |
| 6 | 자동매매 인프라 (PAPER/LIVE, 포지션 영속화, 리스크 관리, 긴급 중단) | ✅ 완료 |

---

## 아키텍처 흐름

### 백테스팅

```
Upbit 캔들 API
    ↓  (OpenFeign + Rate Limit Interceptor)
CandleCollectService  →  DB (Candle 테이블)
                                ↓
                    IndicatorCalculator (TA4J)
                          EMA / RSI / ATR / ADX / Volume
                                ↓
                    Strategy (매매 신호 생성)
                      EMA 스캘핑 v2 (HTF 필터 + ATR 손익)
                                ↓
                    BacktestEngine (시뮬레이션 루프)
                      진입 · 청산 · 수수료 반영
                                ↓
                    PerformanceCalculator
                      수익률 / 최대낙폭 / 승률 / 샤프지수
```

### 자동매매 루프 (PAPER / LIVE)

```
TradingScheduler (cron: 5분봉 마감 30초 후)
    ↓
TradingExecutionService
    ├── EmergencyStopService  — 긴급 중단 플래그 확인
    ├── DailyRiskGuard        — 일일 손실 한도 확인
    ├── LivePositionRepository — 오픈 포지션 조회
    │
    ├─ [포지션 없음] TradingSignalEvaluator.isEntrySignal()
    │       └─ [BUY 시그널] SignalLog 기록 → LIVE이면 OrderGuard + openPosition
    │
    └─ [포지션 있음] TradingSignalEvaluator.isExitSignal()
            └─ [SELL 시그널] SignalLog 기록 → LIVE이면 closePosition + DailyRiskGuard.recordClose()
```

---

## 기술 스택

### 기존

| 항목 | 라이브러리 | 버전 |
|------|-----------|------|
| 언어 | Java | 21 (LTS) |
| 프레임워크 | Spring Boot | 3.4.5 |
| 외부 API 호출 | Spring Cloud OpenFeign | Spring Cloud 2024.0.1 내장 |
| API 문서 | springdoc-openapi | 2.8.6 |
| 빌드 | Gradle | - |

### 추가 (백테스팅 + 자동매매)

| 항목 | 라이브러리 | 역할 |
|------|-----------|------|
| 기술지표 계산 | ta4j-core 0.14 | SMA / EMA / RSI / MACD / 볼린저밴드 등 |
| ORM | spring-boot-starter-data-jpa | 엔터티 ↔ DB 매핑 |
| DB (개발) | H2 (파일 모드) | 서버 재시작 후에도 캔들 데이터 유지, DataGrip 동시 접속 가능 |
| DB (프로덕션) | PostgreSQL | Dialect 교체만으로 전환 가능 |
| 입력 검증 | spring-boot-starter-validation | 요청 파라미터 검증 |

---

## 패키지 구조

```
src/main/java/com/clone/up/
│
├── client/
│   └── UpbitApiClient.java          # 업비트 외부 API Feign 클라이언트
│
├── config/
│   ├── AsyncConfig.java             # candleCollectExecutor (단일 스레드)
│   ├── FeignConfig.java             # Feign 타임아웃 / Rate Limit Interceptor (8 req/sec)
│   ├── TradingProperties.java       # trading.* YAML 바인딩 (@ConfigurationProperties)
│   └── UpbitErrorDecoder.java       # 업비트 API 에러 공통 처리
│
├── domain/
│   ├── market/                      # 시세 도메인
│   │   ├── controller/MarketController.java
│   │   ├── service/MarketService.java
│   │   └── dto/
│   │
│   ├── candle/                      # 캔들 데이터
│   │   ├── entity/Candle.java
│   │   ├── entity/CandleType.java
│   │   ├── dto/UpbitCandleResponse.java
│   │   ├── repository/CandleRepository.java
│   │   ├── service/CandleCollectService.java  # 분/일/주 캔들 수집, 5분마다 자동 수집
│   │   └── controller/CandleController.java
│   │
│   ├── indicator/                   # 기술지표
│   │   └── service/IndicatorCalculationService.java
│   │
│   ├── strategy/                    # 매매 전략
│   │   ├── TradingStrategy.java     # 전략 인터페이스
│   │   ├── StrategyType.java        # SCALPING
│   │   ├── StrategyParam.java       # EMA/RSI/ATR/ADX/Volume/HTF 파라미터
│   │   ├── StrategyFactory.java
│   │   ├── rule/AtrStopLossRule.java    # ATR 기반 손절 룰
│   │   ├── rule/AtrTakeProfitRule.java  # ATR 기반 익절 룰
│   │   ├── rule/CooldownRule.java       # 과매매 방지 쿨다운 룰
│   │   ├── rule/TimeOfDayRule.java      # KST 시간대 필터 룰
│   │   └── service/ScalpingStrategy.java  # EMA 스캘핑 v2
│   │
│   ├── backtest/                    # 백테스팅 엔진
│   │   ├── entity/BacktestResult.java
│   │   ├── entity/Trade.java
│   │   ├── entity/TradeType.java
│   │   ├── entity/PerformanceMetrics.java
│   │   ├── dto/BacktestRequest.java
│   │   ├── dto/BacktestResponse.java
│   │   ├── repository/BacktestResultRepository.java
│   │   ├── service/BacktestExecutionService.java   # 수수료 0.05% 반영
│   │   ├── service/BuyAndHoldBenchmarkService.java
│   │   ├── service/PerformanceAnalysisService.java
│   │   └── controller/BacktestController.java
│   │
│   └── trading/                     # 자동매매 인프라
│       ├── entity/
│       │   ├── LivePosition.java        # 오픈/클로즈 포지션 영속화
│       │   ├── PositionStatus.java      # OPEN | CLOSED
│       │   ├── SignalLog.java           # BUY_SIGNAL / SELL_SIGNAL 이력
│       │   ├── SignalType.java
│       │   ├── DailyTradingRecord.java  # 일일 손실 누계, halted 플래그
│       │   └── TradingMode.java         # PAPER | LIVE
│       ├── dto/
│       │   └── LivePositionResponse.java
│       ├── repository/
│       │   ├── LivePositionRepository.java   # + Pessimistic Lock 쿼리
│       │   ├── SignalLogRepository.java
│       │   └── DailyTradingRecordRepository.java
│       ├── service/
│       │   ├── LivePositionService.java      # openPosition / closePosition
│       │   ├── TradingSignalEvaluator.java   # TA4J entry/exit rule 평가
│       │   ├── TradingExecutionService.java  # 자동매매 메인 로직
│       │   ├── DailyRiskGuard.java           # 일일 손실 한도 & halted 관리
│       │   ├── EmergencyStopService.java     # AtomicBoolean 긴급 중단
│       │   └── OrderGuard.java              # ConcurrentHashMap 중복 주문 방지
│       ├── scheduler/
│       │   └── TradingScheduler.java         # cron: 5분봉 마감 30초 후
│       └── controller/
│           ├── LivePositionController.java   # /api/positions
│           ├── SignalLogController.java      # /api/signals
│           └── EmergencyStopController.java  # /api/trading/emergency-stop
│
├── global/
│   ├── exception/
│   │   ├── ErrorCode.java
│   │   ├── UpException.java
│   │   └── GlobalExceptionHandler.java
│   └── response/
│       └── ApiResponse.java
│
└── UpApplication.java
```

---

## API 엔드포인트

### 시세 / 캔들

| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/v1/market/ticker?markets=KRW-BTC` | 실시간 시세 조회 |
| GET | `/api/v1/market/pair` | 페어 목록 조회 |
| POST | `/api/v1/candles/minutes` | 분 캔들 수집 (market, unit, count) |
| POST | `/api/v1/candles/days` | 일 캔들 수집 |
| POST | `/api/v1/candles/weeks` | 주 캔들 수집 |

### 백테스팅

| Method | URL | 설명 |
|--------|-----|------|
| POST | `/api/v1/backtests` | 백테스팅 실행 |
| GET | `/api/v1/backtests/{id}` | 백테스팅 결과 조회 |

### 자동매매

| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/positions?market=KRW-BTC` | 마켓별 포지션 이력 조회 |
| GET | `/api/positions/open` | 전체 오픈 포지션 목록 |
| GET | `/api/signals?market=KRW-BTC` | 마켓별 최근 시그널 50개 조회 |
| POST | `/api/trading/emergency-stop?reason=` | 긴급 거래 중단 활성화 |
| DELETE | `/api/trading/emergency-stop` | 긴급 중단 해제 |
| GET | `/api/trading/emergency-stop` | 긴급 중단 상태 조회 |

---

## 자동매매 설정 (application.yaml)

```yaml
trading:
  mode: PAPER                    # LIVE | PAPER (기본: PAPER — 시그널 기록만)
  market: KRW-BTC
  initial-capital: 1000000       # 일일 손실 한도 계산 기준 (단위: 원)
  invest-ratio: 1.0              # 매수 시 자본 투입 비율 (0~1)
  daily-loss-limit-percent: 3.0  # 일일 손실 한도 (%)
  candle-warmup-count: 200       # 지표 계산 최소 캔들 수
  scheduler-enabled: false       # true 시 자동매매 스케줄러 활성화 (로컬은 false)
  # EMA 스캘핑 v2 파라미터
  ema-short-period: 9
  ema-long-period: 21
  rsi-period: 14
  rsi-momentum: 50               # RSI 기준선 (50 = 모멘텀 방향 확인)
  atr-period: 14
  atr-stop-multiplier: 1.5       # 손절: 진입가 - 1.5×ATR
  atr-take-multiplier: 2.5       # 익절: 진입가 + 2.5×ATR
  adx-period: 14
  adx-threshold: 25              # 횡보장 차단 기준
  volume-sma-period: 20
  volume-multiplier: 1.2         # 볼륨 스파이크 기준 (SMA × 1.2)
  htf-ema-period: 288            # 288 × 5분 = 24H 추세 프록시
  trade-start-hour: 9            # KST 거래 허용 시작 시각
  trade-end-hour: 22             # KST 거래 허용 종료 시각
```

### PAPER vs LIVE 모드

| 항목 | PAPER | LIVE |
|------|-------|------|
| 시그널 로그 기록 | ✅ | ✅ |
| 포지션 영속화 (LivePosition) | ✅ | ✅ |
| 실제 주문 API 호출 | ❌ | ✅ (추후 연동) |

---

## 리스크 관리

| 기능 | 구현 방식 |
|------|----------|
| 일일 손실 한도 | `DailyRiskGuard` — DailyTradingRecord.isHalted() 기준, 한도 초과 시 당일 진입 차단 |
| 중복 주문 방지 (JVM) | `OrderGuard` — ConcurrentHashMap.newKeySet() + try-with-resources |
| 중복 주문 방지 (DB) | `LivePositionRepository` — `@Lock(PESSIMISTIC_WRITE)` JPQL 쿼리 |
| 긴급 중단 | `EmergencyStopService` — AtomicBoolean 플래그, REST API로 토글 |

---

## 공통 응답 포맷

```json
// 성공
{ "success": true, "data": { ... }, "error": null }

// 실패
{ "success": false, "data": null, "error": "에러 메시지" }
```

## 에러 처리 구조

```
외부 API 에러 → UpbitErrorDecoder → UpException → GlobalExceptionHandler → ApiResponse
```
