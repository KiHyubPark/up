# Up

Upbit 시세 데이터를 조회하는 Spring Boot 기반 REST API 서버

## 기술 스택

| 항목 | 버전 |
|---|---|
| Java | 21 (LTS) |
| Spring Boot | 3.4.5 |
| Spring Cloud | 2024.0.1 |
| Spring Cloud OpenFeign | Spring Cloud 내장 |
| Lombok | Spring Boot 내장 |
| 빌드 | Gradle |

## 패키지 구조

```
src/main/java/com/clone/up/
│
├── client/
│   └── UpbitApiClient.java          # 업비트 외부 API Feign 클라이언트
│
├── config/
│   ├── FeignConfig.java             # Feign 타임아웃, 로그 레벨 설정
│   └── UpbitErrorDecoder.java       # 업비트 API 에러 공통 처리
│
├── domain/
│   └── upbit/                       # 업비트 도메인
│       ├── controller/
│       │   └── UpbitController.java
│       ├── service/
│       │   └── UpbitService.java
│       └── dto/
│           ├── UpbitTickerResponse.java
│           └── UpbitPairResponse.java
│
├── global/
│   ├── exception/
│   │   ├── ErrorCode.java           # 에러 코드 enum
│   │   ├── UpException.java         # 공통 비즈니스 예외
│   │   └── GlobalExceptionHandler.java
│   └── response/
│       └── ApiResponse.java         # 공통 응답 포맷
│
└── UpApplication.java
```

## API 엔드포인트

| Method | URL | 설명 |
|---|---|---|
| GET | `/api/v1/upbit/ticker?markets=KRW-BTC` | 시세 조회 |
| GET | `/api/v1/upbit/pair` | 페어 목록 조회 |

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
