package com.clone.up.global.exception;

import com.clone.up.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
        return ResponseEntity
                .internalServerError()
                .body(ApiResponse.error("서버 오류입니다"));
    }
}
