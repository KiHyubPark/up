package com.clone.up.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

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
