package com.clone.up.domain.upbit.dto;

public record UpbitPairResponse(
        String market,
        String korean_name,
        String english_name
) {
}
