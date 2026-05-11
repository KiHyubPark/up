package com.clone.up.domain.backtest.controller;

import com.clone.up.domain.backtest.dto.BacktestRequest;
import com.clone.up.domain.backtest.dto.BacktestResponse;
import com.clone.up.domain.backtest.service.BacktestExecutionService;
import com.clone.up.domain.candle.entity.CandleType;
import com.clone.up.domain.strategy.StrategyType;
import com.clone.up.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Backtest", description = "백테스팅 API")
@RestController
@RequestMapping("/api/v1/backtests")
@Validated
public class BacktestController {

    private final BacktestExecutionService executionService;

    public BacktestController(BacktestExecutionService executionService) {
        this.executionService = executionService;
    }

    @Operation(summary = "백테스트 실행", description = "지정한 마켓/기간/전략으로 백테스트를 실행합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<BacktestResponse>> run(
            @Valid @RequestBody BacktestRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(executionService.run(request)));
    }

    @Operation(summary = "백테스트 결과 조회", description = "ID로 백테스트 결과를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BacktestResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(executionService.findById(id)));
    }

    @Operation(
            summary = "백테스트 이력 조회",
            description = "마켓 단위 또는 마켓+캔들타입+전략 조합으로 백테스트 이력을 최신순으로 조회합니다."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<BacktestResponse>>> findHistory(
            @RequestParam @NotBlank String market,
            @RequestParam(required = false) CandleType candleType,
            @RequestParam(required = false) StrategyType strategyType) {
        return ResponseEntity.ok(ApiResponse.ok(
                executionService.findHistory(market, candleType, strategyType)));
    }

    @Operation(summary = "전략 비교", description = "여러 전략을 동시에 백테스트하여 결과를 비교합니다.")
    @PostMapping("/compare")
    public ResponseEntity<ApiResponse<List<BacktestResponse>>> compare(
            @Valid @RequestBody List<@Valid BacktestRequest> requests) {
        return ResponseEntity.ok(ApiResponse.ok(executionService.runCompare(requests)));
    }
}
