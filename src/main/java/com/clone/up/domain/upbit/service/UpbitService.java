package com.clone.up.domain.upbit.service;

import com.clone.up.client.UpbitApiClient;
import com.clone.up.domain.upbit.dto.UpbitTickerResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpbitService {

    private final UpbitApiClient upbitApiClient;

    public List<UpbitTickerResponse> getTicker() {
        return upbitApiClient.getTicker();
    }
}
