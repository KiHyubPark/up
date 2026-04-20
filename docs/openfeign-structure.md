# OpenFeign 프로젝트 구조

## 패키지 구조

```
src/main/java/com/clone/up/
├── client/                          # Feign 클라이언트 인터페이스
│   └── ExampleApiClient.java
│
├── config/                          # 공통 설정
│   └── FeignConfig.java             # 타임아웃, 로그 레벨
│
├── domain/                          # 비즈니스 도메인 (API 단위로 패키지 분리)
│   └── example/
│       ├── ExampleService.java      # Feign 클라이언트 주입 및 비즈니스 로직
│       └── dto/
│           └── ExampleResponse.java
│
└── UpApplication.java               # @EnableFeignClients 선언
```

## 주요 파일 역할

| 파일 | 역할 |
|---|---|
| `client/*ApiClient.java` | 외부 API 엔드포인트 정의 |
| `config/FeignConfig.java` | 타임아웃, 로그 레벨 등 공통 설정 |
| `domain/*/Service.java` | Feign 클라이언트를 주입받아 비즈니스 로직 처리 |
| `domain/*/dto/` | 요청/응답 DTO (record 사용) |

## 새 외부 API 추가 방법

1. `client/` 에 `XxxApiClient.java` 인터페이스 추가
2. `domain/xxx/` 패키지 생성 후 `XxxService.java` 작성
3. `domain/xxx/dto/` 에 요청/응답 record 추가
4. `application.yaml` 에 URL 환경변수 추가

```yaml
feign:
  xxx-api:
    url: ${XXX_API_URL:http://localhost:8080}
```

## 설정 값

| 설정 | 기본값 | 설명 |
|---|---|---|
| `connectTimeout` | 5,000ms | 연결 타임아웃 |
| `readTimeout` | 10,000ms | 읽기 타임아웃 |
| Feign 로그 레벨 | BASIC | 요청 URL, 응답 코드, 소요시간 |

## 로그 레벨 종류

| 레벨 | 출력 내용 |
|---|---|
| `NONE` | 로그 없음 |
| `BASIC` | 요청 메서드, URL, 응답 코드, 소요시간 |
| `HEADERS` | BASIC + 헤더 |
| `FULL` | HEADERS + 요청/응답 바디 |
