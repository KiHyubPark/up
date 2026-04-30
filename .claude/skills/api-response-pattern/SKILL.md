---
name: api-response-pattern
description: |
  이 프로젝트의 API 응답 패턴 가이드. ApiResponse, ErrorCode, UpException, GlobalExceptionHandler 연결 구조를 설명한다.
  새 API 엔드포인트 작성, 예외 처리 추가, 새 ErrorCode 정의 시 반드시 이 스킬을 사용한다.
  컨트롤러에서 응답을 어떻게 반환해야 하는지, 예외를 어떻게 던져야 하는지 물어보면 이 스킬을 적용한다.
---

# API 응답 패턴

이 프로젝트는 모든 API 응답을 `ApiResponse<T>` 레코드로 감싸고, 예외는 `UpException` → `GlobalExceptionHandler` 체인으로 처리한다.

## 핵심 클래스 위치

```
com.clone.up.global.response.ApiResponse       # 응답 래퍼
com.clone.up.global.exception.ErrorCode        # 에러 코드 열거형
com.clone.up.global.exception.UpException      # 도메인 예외
com.clone.up.global.exception.GlobalExceptionHandler  # 전역 예외 핸들러
```

## ApiResponse 구조

```java
public record ApiResponse<T>(
        boolean success,
        T data,
        String error
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message);
    }
}
```

- 성공: `success=true`, `data=응답데이터`, `error=null`
- 실패: `success=false`, `data=null`, `error=메시지`

## ErrorCode 열거형

```java
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력입니다"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류입니다"),

    // Upbit
    UPBIT_INVALID_MARKET(HttpStatus.BAD_REQUEST, "잘못된 마켓 코드입니다"),
    UPBIT_RATE_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "업비트 API 요청 한도를 초과했습니다"),
    UPBIT_API_ERROR(HttpStatus.BAD_GATEWAY, "업비트 API 호출에 실패했습니다");

    private final HttpStatus status;
    private final String message;
}
```

새 도메인 에러를 추가할 때는 이 파일에 상수를 추가한다. 카테고리 주석(`// Upbit`, `// 공통`)을 유지한다.

## UpException 사용

```java
// ErrorCode 기반 (권장) — HTTP 상태와 메시지가 ErrorCode에 정의된 대로 자동 설정
throw new UpException(ErrorCode.NOT_FOUND);

// 직접 지정 — 특정 동적 메시지가 필요할 때
throw new UpException(HttpStatus.BAD_REQUEST, "마켓 코드 " + market + "은(는) 존재하지 않습니다");
```

## 컨트롤러 응답 패턴

```java
@RestController
@RequestMapping("/api/v1/markets")
@RequiredArgsConstructor
public class MarketController {

    private final MarketService marketService;

    // 성공 응답
    @GetMapping("/ticker")
    public ApiResponse<List<TickerResponse>> getTicker(@RequestParam String markets) {
        return ApiResponse.ok(marketService.getTicker(markets));
    }

    // 예외는 서비스 계층에서 throw — 컨트롤러는 try/catch 하지 않는다
}
```

컨트롤러는 `try/catch` 없이 `ApiResponse.ok(...)` 만 반환한다. 예외 변환은 `GlobalExceptionHandler`가 담당한다.

## GlobalExceptionHandler 동작

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    // UpException → 해당 HTTP 상태 + 에러 메시지 반환
    @ExceptionHandler(UpException.class)
    public ResponseEntity<ApiResponse<?>> handleUpException(UpException e) {
        return ResponseEntity
                .status(e.getHttpStatus())
                .body(ApiResponse.error(e.getMessage()));
    }

    // 예상치 못한 예외 → 500 + 고정 메시지
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception ignored) {
        return ResponseEntity
                .internalServerError()
                .body(ApiResponse.error("서버 오류입니다"));
    }
}
```

`Exception` 핸들러는 내부 예외 정보를 클라이언트에 노출하지 않는다. 상세 로그는 서버에만 남긴다.

## 새 도메인 에러 추가 절차

1. `ErrorCode`에 상수 추가 (카테고리 주석 아래)
2. 서비스에서 `throw new UpException(ErrorCode.새코드)` 사용
3. Feign 에러는 `UpbitErrorDecoder`에서 처리 — 도메인 서비스에서 별도 처리 불필요

## 응답 예시

```json
// 성공
{ "success": true, "data": [...], "error": null }

// 실패
{ "success": false, "data": null, "error": "잘못된 마켓 코드입니다" }
```
