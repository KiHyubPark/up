package com.clone.up.global.exception;

import com.clone.up.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// @RestControllerAdvice : 전체 컨트롤러에서 발생하는 예외를 한 곳에서 처리
//   → @ControllerAdvice + @ResponseBody 의 합성
//   → @ExceptionHandler 메서드의 반환값을 JSON으로 직렬화해서 응답 바디에 담음
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UpException.class)
    public ResponseEntity<ApiResponse<?>> handleUpException(UpException e) {
        return ResponseEntity
                .status(e.getHttpStatus())
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity
                .internalServerError()
                .body(ApiResponse.error("서버 오류입니다"));
    }
}
