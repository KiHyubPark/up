package com.clone.up.client;

import com.clone.up.domain.candle.dto.UpbitCandleResponse;
import com.clone.up.domain.market.dto.PairResponse;
import com.clone.up.domain.market.dto.TickerResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
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

    // 분 캔들 (unit: 1, 5, 15, 60, 240)
    @RequestMapping(method = RequestMethod.GET, value = "/v1/candles/minutes/{unit}")
    List<UpbitCandleResponse> getMinuteCandles(
            @PathVariable int unit,
            @RequestParam String market,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) Integer count);

    // 일 캔들
    @RequestMapping(method = RequestMethod.GET, value = "/v1/candles/days")
    List<UpbitCandleResponse> getDayCandles(
            @RequestParam String market,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) Integer count);

    // 주 캔들
    @RequestMapping(method = RequestMethod.GET, value = "/v1/candles/weeks")
    List<UpbitCandleResponse> getWeekCandles(
            @RequestParam String market,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) Integer count);
}
