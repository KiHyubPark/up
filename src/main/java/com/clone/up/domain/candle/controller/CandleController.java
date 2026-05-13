package com.clone.up.domain.candle.controller;

import com.clone.up.domain.candle.service.CandleCollectService;
import com.clone.up.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Candle", description = "캔들 데이터 수집 API")
@RestController
@RequestMapping("/api/v1/candles")
@RequiredArgsConstructor
public class CandleController {

    private final CandleCollectService candleCollectService;

    @Operation(summary = "분 캔들 수집", description = "업비트에서 분 캔들 데이터를 수집하여 저장합니다.")
    @PostMapping("/minutes")
    public ApiResponse<Integer> collectMinuteCandles(
            @RequestParam(defaultValue = "KRW-BTC") String market,
            @RequestParam(defaultValue = "60") int unit,
            @RequestParam(defaultValue = "200") int count
    ) {
        return ApiResponse.ok(candleCollectService.collectMinuteCandles(market, unit, count));
    }

    @Operation(
            summary = "분 캔들 비동기 수집",
            description = "업비트에서 분 캔들을 백그라운드로 수집합니다. 즉시 202를 반환하며 서버 로그에서 진행 상황을 확인할 수 있습니다."
    )
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/minutes/async")
    public ApiResponse<String> collectMinuteCandlesAsync(
            @RequestParam(defaultValue = "KRW-BTC") String market,
            @RequestParam(defaultValue = "5") int unit,
            @RequestParam(defaultValue = "52000") int count
    ) {
        candleCollectService.collectMinuteCandlesAsync(market, unit, count);
        return ApiResponse.ok(
                "수집이 시작되었습니다. market=%s, unit=%d, count=%d".formatted(market, unit, count)
        );
    }

    @Operation(summary = "일 캔들 수집", description = "업비트에서 일 캔들 데이터를 수집하여 저장합니다.")
    @PostMapping("/days")
    public ApiResponse<Integer> collectDayCandles(
            @RequestParam(defaultValue = "KRW-BTC") String market,
            @RequestParam(defaultValue = "200") int count
    ) {
        return ApiResponse.ok(candleCollectService.collectDayCandles(market, count));
    }

    @Operation(summary = "주 캔들 수집", description = "업비트에서 주 캔들 데이터를 수집하여 저장합니다.")
    @PostMapping("/weeks")
    public ApiResponse<Integer> collectWeekCandles(
            @RequestParam(defaultValue = "KRW-BTC") String market,
            @RequestParam(defaultValue = "200") int count
    ) {
        return ApiResponse.ok(candleCollectService.collectWeekCandles(market, count));
    }
}
