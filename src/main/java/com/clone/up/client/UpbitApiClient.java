package com.clone.up.client;

import com.clone.up.domain.upbit.dto.UpbitTickerResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

// Base URL만 받도록 되어있음
@FeignClient(name = "upbit", url = "https://api.upbit.com")
public interface UpbitApiClient {
    @RequestMapping(method = RequestMethod.GET, value = "/v1/ticker?markets=KRW-BTC")
    List<UpbitTickerResponse> getTicker();
}
