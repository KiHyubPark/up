package com.clone.up.domain.market.service;

import com.clone.up.client.UpbitApiClient;
import com.clone.up.domain.market.dto.PairResponse;
import com.clone.up.domain.market.dto.TickerResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketService {

    private final UpbitApiClient upbitApiClient;

    public List<TickerResponse> getTicker(String markets) {
        return upbitApiClient.getTicker(markets);
    }

    public List<PairResponse> getPairs() {
        return upbitApiClient.getPairs();
    }
}
