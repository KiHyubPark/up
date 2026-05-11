# Up

Upbit API를 기반으로 코인 시세 조회 및 백테스팅을 수행하는 Spring Boot REST API 서버

## 프로젝트 목표

업비트(Upbit) 거래소 API를 활용하여 **KRW-BTC 등 코인 쌍에 대한 매매 전략 백테스팅 플랫폼**을 구축한다.

과거 OHLCV(시가·고가·저가·종가·거래량) 캔들 데이터를 수집·저장하고, TA4J 기반 기술지표(SMA, EMA, RSI, MACD, 볼린저밴드)를 계산하여 골든크로스·RSI 과매도·MACD 크로스 등 전략을 시뮬레이션한다. 시뮬레이션 결과로 수익률·최대낙폭·승률·샤프지수 등 성과지표를 제공한다.

### 구현 단계

| Phase | 내용 | 상태 |
|-------|------|------|
| 0 | Upbit 시세 조회 API (ticker, pair) | ✅ 완료 |
| 1 | 캔들 API 추가 + Candle 엔터티 + DB 저장 + 스케줄 수집 | ✅ 완료 |
| 2 | TA4J 통합 + 기술지표 계산 서비스 (SMA / EMA / RSI / MACD / BB) | ✅ 완료 |
| 3 | 매매 전략 구현 (골든크로스, RSI 과매도, MACD 크로스, 볼린저밴드, 스캘핑) | ✅ 완료 |
| 4 | 백테스팅 엔진 (시뮬레이션 루프 + 수수료 반영) | ✅ 완료 |
| 5 | 성과지표 계산 + REST API 노출 | ✅ 완료 |

---

## 아키텍처 흐름

```
Upbit 캔들 API
    ↓  (OpenFeign + Rate Limit Interceptor)
CandleCollectService  →  DB (Candle 테이블)
                                ↓
                    IndicatorCalculator (TA4J)
                          SMA / EMA / RSI / MACD / BB
                                ↓
                    Strategy (매매 신호 생성)
                      골든크로스 / RSI 과매도 / MACD 크로스
                                ↓
                    BacktestEngine (시뮬레이션 루프)
                      진입 · 청산 · 수수료 반영
                                ↓
                    PerformanceCalculator
                      수익률 / 최대낙폭 / 승률 / 샤프지수
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

### 추가 (백테스팅)

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
│   ├── FeignConfig.java             # Feign 타임아웃 / Rate Limit Interceptor (8 req/sec)
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
│   │   ├── StrategyType.java        # GOLDEN_CROSS / RSI_OVERSOLD / MACD_CROSS / BOLLINGER_BAND / SCALPING
│   │   ├── StrategyParam.java
│   │   ├── StrategyFactory.java
│   │   ├── service/GoldenCrossStrategy.java
│   │   ├── service/RsiOversoldStrategy.java
│   │   ├── service/MacdCrossStrategy.java
│   │   ├── service/BollingerBandStrategy.java
│   │   └── service/ScalpingStrategy.java
│   │
│   └── backtest/                    # 백테스팅 엔진
│       ├── entity/BacktestResult.java
│       ├── entity/Trade.java
│       ├── entity/TradeType.java
│       ├── entity/PerformanceMetrics.java
│       ├── dto/BacktestRequest.java
│       ├── dto/BacktestResponse.java
│       ├── repository/BacktestResultRepository.java
│       ├── service/BacktestExecutionService.java   # 수수료 0.05% 반영
│       ├── service/BuyAndHoldBenchmarkService.java
│       ├── service/PerformanceAnalysisService.java
│       └── controller/BacktestController.java
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

| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/v1/market/ticker?markets=KRW-BTC` | 실시간 시세 조회 |
| GET | `/api/v1/market/pair` | 페어 목록 조회 |
| POST | `/api/v1/candles/minutes` | 분 캔들 수집 (market, unit, count) |
| POST | `/api/v1/candles/days` | 일 캔들 수집 |
| POST | `/api/v1/candles/weeks` | 주 캔들 수집 |
| POST | `/api/v1/backtests` | 백테스팅 실행 |
| GET | `/api/v1/backtests/{id}` | 백테스팅 결과 조회 |

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
