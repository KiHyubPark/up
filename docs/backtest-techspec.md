# 백테스팅 기술 스펙

## 개요

KRW-BTC 백테스팅 시스템 — 과거 캔들 데이터(OHLCV)를 기반으로 매매 전략을 시뮬레이션하여 수익률, 최대낙폭, 승률 등 성과지표를 분석하는 시스템.

현재 프로젝트는 Upbit 시세 조회 API만 구현된 상태. 아래 기술 스택을 추가하여 백테스팅 플랫폼으로 확장한다.

---

## 전체 기술 스택

### 기존 (유지)

| 항목 | 라이브러리 | 버전 |
|------|-----------|------|
| 언어 | Java | 21 (LTS) |
| 프레임워크 | Spring Boot | 3.4.5 |
| 외부 API 호출 | Spring Cloud OpenFeign | Spring Cloud 2024.0.1 내장 |
| API 문서 | springdoc-openapi | 2.8.6 |
| 빌드 | Gradle | - |

### 추가 (백테스팅)

| 항목 | 라이브러리 | 버전 | 역할 |
|------|-----------|------|------|
| 기술지표 계산 | ta4j-core | 0.14 | SMA / EMA / RSI / MACD / 볼린저밴드 등 |
| ORM | spring-boot-starter-data-jpa | Spring Boot 내장 | 엔터티 ↔ DB 매핑 |
| DB (개발) | h2 | Spring Boot 내장 | 인메모리 DB, 별도 설치 불필요 |
| DB (프로덕션) | postgresql | Spring Boot 내장 | 추후 전환 시 Dialect만 변경 |
| 입력 검증 | spring-boot-starter-validation | Spring Boot 내장 | 요청 파라미터 검증 |

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

## 추가 필요한 Upbit API

현재 구현: `/v1/ticker`, `/v1/market/all`

| API | 설명 | 주요 파라미터 |
|-----|------|-------------|
| `GET /v1/candles/minutes/{unit}` | 분 캔들 (1, 5, 15, 60, 240분) | market, to(종료 시점), count(최대 200) |
| `GET /v1/candles/days` | 일 캔들 | market, to, count |
| `GET /v1/candles/weeks` | 주 캔들 | market, to, count |

> **Rate Limit**: Upbit API는 초당 최대 30회 요청 허용.
> Feign Interceptor를 추가하여 요청 간격을 자동으로 제어한다.

---

## 패키지 구조 (추가 부분)

```
com.clone.up
├── client/
│   └── UpbitApiClient.java       # 캔들 API 3개 추가
│
├── config/
│   └── FeignConfig.java          # Rate Limit Interceptor 추가
│
├── domain/
│   ├── candle/                   # 캔들 데이터
│   │   ├── entity/Candle.java
│   │   ├── dto/UpbitCandleResponse.java
│   │   ├── repository/CandleRepository.java
│   │   ├── service/CandleCollectService.java
│   │   └── controller/CandleController.java
│   │
│   ├── indicator/                # 기술지표
│   │   └── service/IndicatorCalculator.java
│   │
│   ├── strategy/                 # 매매 전략
│   │   ├── service/GoldenCrossStrategy.java
│   │   ├── service/RsiStrategy.java
│   │   └── service/MacdStrategy.java
│   │
│   └── backtest/                 # 백테스팅 엔진
│       ├── entity/BacktestResult.java
│       ├── entity/Trade.java
│       ├── entity/PerformanceMetrics.java
│       ├── service/BacktestEngine.java
│       ├── service/PerformanceCalculator.java
│       └── controller/BacktestController.java
│
└── infrastructure/
    └── scheduler/CandleScheduler.java   # @Scheduled 수집
```

---

## 핵심 엔터티

### Candle (캔들 데이터)

| 필드 | 타입 | 설명 |
|------|------|------|
| market | String | 마켓 코드 (KRW-BTC) |
| candleTime | LocalDateTime | 캔들 시작 시간 (UTC) |
| timeUnit | Enum | MINUTE_1 / MINUTE_5 / HOUR_1 / DAY_1 등 |
| openPrice | BigDecimal | 시가 |
| highPrice | BigDecimal | 고가 |
| lowPrice | BigDecimal | 저가 |
| closePrice | BigDecimal | 종가 |
| volume | BigDecimal | 거래량 (BTC) |
| tradedValue | BigDecimal | 거래대금 (KRW) |

### BacktestResult (시뮬레이션 결과)

| 필드 | 타입 | 설명 |
|------|------|------|
| strategy | Strategy | 적용 전략 |
| market | String | KRW-BTC |
| startDate / endDate | LocalDateTime | 백테스팅 기간 |
| initialCapital | BigDecimal | 초기 투자금 (KRW) |
| finalValue | BigDecimal | 최종 평가액 (KRW) |
| trades | List\<Trade\> | 개별 거래 기록 |
| performanceMetrics | PerformanceMetrics | 성과지표 |

### Trade (거래 기록)

| 필드 | 타입 | 설명 |
|------|------|------|
| tradeType | Enum | BUY / SELL |
| tradeTime | LocalDateTime | 거래 시각 |
| price | BigDecimal | 체결 가격 |
| quantity | BigDecimal | 수량 (BTC) |
| commission | BigDecimal | 수수료 (Upbit 0.05%) |

### PerformanceMetrics (성과지표)

| 필드 | 설명 |
|------|------|
| totalReturn | 총 수익률 (%) |
| maxDrawdown | 최대낙폭 (%) |
| winRate | 승률 (%) |
| sharpeRatio | 샤프지수 |
| totalTrades | 총 거래 횟수 |

---

## 기술 선택 근거

| 항목 | 선택 | 이유 |
|------|------|------|
| 기술지표 | TA4J | 직접 구현 대신 검증된 계산, Java 생태계 표준, 50+ 지표 내장 |
| DB | H2 (개발) | 백테스팅은 로컬 실행이 많으므로 설치 불필요한 내장 DB로 시작 |
| DB 전환 | PostgreSQL | JPA Dialect만 교체하면 마이그레이션 완료 |
| 스케줄링 | @Scheduled | 캔들 수집은 단순 반복 작업 → 배치보다 경량한 방식으로 시작 |
| ORM | JPA | 도메인 모델 중심 설계, 추후 DB 변경 시 코드 수정 최소화 |

---

## 구현 단계

| Phase | 내용 |
|-------|------|
| 1 | Upbit 캔들 API 추가 + Candle 엔터티 + DB 저장 + @Scheduled 수집 |
| 2 | TA4J 통합 + SMA / EMA / RSI / MACD / BB 지표 계산 서비스 |
| 3 | 전략 구현 (골든크로스, RSI 과매도, MACD 크로스) |
| 4 | 백테스팅 엔진 (시뮬레이션 루프 + 수수료 반영) |
| 5 | 성과지표 계산 + REST API 노출 |