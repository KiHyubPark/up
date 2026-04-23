package com.clone.up.config;

import com.clone.up.global.exception.ErrorCode;
import com.clone.up.global.exception.UpException;
import feign.Response;
import feign.codec.ErrorDecoder;

public class UpbitErrorDecoder implements ErrorDecoder {
    @Override
    public Exception decode(String methodKey, Response response) {
        return switch (response.status()) {
            case 400 -> new UpException(ErrorCode.UPBIT_INVALID_MARKET);
            case 429 -> new UpException(ErrorCode.UPBIT_RATE_LIMIT);
            default  -> new UpException(ErrorCode.UPBIT_API_ERROR);
        };
    }
}
