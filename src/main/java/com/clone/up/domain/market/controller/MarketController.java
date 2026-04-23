package com.clone.up.domain.market.controller;

import com.clone.up.domain.market.dto.PairResponse;
import com.clone.up.domain.market.dto.TickerResponse;
import com.clone.up.domain.market.service.MarketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/market")
public class MarketController {

    private final MarketService marketService;

    @GetMapping("/ticker")
    public List<TickerResponse> getTicker(@RequestParam String markets) {
        return marketService.getTicker(markets);
    }

    @GetMapping("/pair")
    public List<PairResponse> getPair() {
        return marketService.getPairs();
    }
}
