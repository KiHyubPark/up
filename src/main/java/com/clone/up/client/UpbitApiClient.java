package com.clone.up.client;

import com.clone.up.domain.market.dto.PairResponse;
import com.clone.up.domain.market.dto.TickerResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

// Base URL만 받도록 되어있음
@FeignClient(name = "upbit", url = "${feign.upbit.base-url}")
public interface UpbitApiClient {

    // 종목코드
    @RequestMapping(method = RequestMethod.GET, value = "/v1/ticker")
    List<TickerResponse> getTicker(@RequestParam String markets);

    // 페어목록
    @RequestMapping(method = RequestMethod.GET, value = "/v1/market/all")
    List<PairResponse> getPairs();
}
