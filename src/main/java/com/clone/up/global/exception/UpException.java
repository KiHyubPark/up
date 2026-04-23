package com.clone.up.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class UpException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String message;

    public UpException(HttpStatus httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public UpException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.httpStatus = errorCode.getStatus();
        this.message = errorCode.getMessage();
    }
}
