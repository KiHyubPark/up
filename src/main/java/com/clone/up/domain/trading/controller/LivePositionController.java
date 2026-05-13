package com.clone.up.domain.trading.controller;

import com.clone.up.domain.strategy.StrategyType;
import com.clone.up.domain.trading.dto.LivePositionResponse;
import com.clone.up.domain.trading.service.LivePositionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Live Position", description = "자동매매 포지션 조회 API")
@RestController
@RequestMapping("/api/positions")
public class LivePositionController {

    private final LivePositionService service;

    public LivePositionController(LivePositionService service) {
        this.service = service;
    }

    @Operation(summary = "전체 OPEN 포지션 조회", description = "현재 보유 중인 모든 포지션을 반환한다.")
    @GetMapping("/open")
    public List<LivePositionResponse> getOpenPositions() {
        return service.findAllOpenPositions().stream()
                .map(LivePositionResponse::from)
                .toList();
    }

    @Operation(summary = "마켓별 포지션 이력 조회", description = "특정 마켓의 전체 거래 이력을 최신 순으로 반환한다.")
    @GetMapping
    public List<LivePositionResponse> getHistory(@RequestParam String market) {
        return service.findHistory(market).stream()
                .map(LivePositionResponse::from)
                .toList();
    }

    @Operation(summary = "포지션 단건 조회")
    @GetMapping("/{id}")
    public ResponseEntity<LivePositionResponse> getById(@PathVariable Long id) {
        return service.findById(id)
                .map(LivePositionResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "마켓+전략의 현재 OPEN 포지션 조회")
    @GetMapping("/open/{market}/{strategy}")
    public ResponseEntity<LivePositionResponse> getOpenPosition(
            @PathVariable String market,
            @PathVariable StrategyType strategy) {
        return service.findOpenPosition(market, strategy)
                .map(LivePositionResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
