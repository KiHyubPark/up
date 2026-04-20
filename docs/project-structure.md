# 프로젝트 구조

## 기술 스택

| 항목 | 버전 |
|---|---|
| Spring Boot | 3.4.5 |
| Spring Cloud | 2024.0.1 |
| Java | 21 (LTS) |
| 빌드 | Gradle |
| 주요 라이브러리 | Spring Cloud OpenFeign, Lombok |

## 패키지 구조

```
src/main/java/com/clone/up/
│
├── client/                          # Feign 클라이언트 인터페이스
│   └── ExampleApiClient.java        # 외부 API 호출 정의 (샘플)
│
├── config/                          # Spring 설정
│   └── FeignConfig.java             # Feign 타임아웃, 로그 레벨 설정
│
├── domain/                          # 비즈니스 도메인 (도메인 단위로 패키지 분리)
│   ├── up/                          # up 도메인
│   │   ├── controller/
│   │   │   └── UpController.java    # REST API 엔드포인트 (/api/v1/up)
│   │   ├── service/                 # 비즈니스 로직 (추가 예정)
│   │   └── dto/                     # 요청/응답 DTO (추가 예정)
│   │
│   └── example/                     # 샘플 도메인 (제거 예정)
│       ├── ExampleService.java
│       └── dto/
│           └── ExampleResponse.java
│
├── global/                          # 전역 공통 처리 (추가 예정)
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java   # 공통 예외 처리
│   │   ├── BusinessException.java        # 공통 비즈니스 예외
│   │   └── ErrorCode.java               # 에러 코드 enum
│   └── response/
│       └── ApiResponse.java             # 공통 응답 포맷
│
└── UpApplication.java               # 애플리케이션 진입점 (@EnableFeignClients)
```

## 패키지 역할

| 패키지 | 역할 |
|---|---|
| `client` | Feign 클라이언트 인터페이스 (외부 API 엔드포인트 정의) |
| `config` | Spring Bean 설정 (Feign, Security 등) |
| `domain/{name}/controller` | REST API 엔드포인트 |
| `domain/{name}/service` | 비즈니스 로직, Feign 클라이언트 호출 |
| `domain/{name}/dto` | 요청/응답 DTO (record 사용) |
| `global/exception` | 공통 예외 처리 |
| `global/response` | 공통 응답 포맷 |

## URL 규칙

```
/api/v1/{도메인명}
```

- 버전은 URL 경로로 관리 (`v1`, `v2`)
- 폴더 구조는 도메인 기준, 버전은 어노테이션으로 관리

## 새 도메인 추가 방법

1. `domain/{name}/` 패키지 생성
2. `controller/`, `service/`, `dto/` 하위 패키지 추가
3. 외부 API 호출이 필요하면 `client/{Name}ApiClient.java` 추가
4. `application.yaml` 에 URL 환경변수 추가
