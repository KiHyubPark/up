package com.clone.up.domain.trading.controller;

import com.clone.up.domain.trading.entity.SignalLog;
import com.clone.up.domain.trading.repository.SignalLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 시그널 로그 조회 API.
 *
 * <ul>
 *   <li>GET /api/signals?market= — 최근 50개 시그널 목록</li>
 * </ul>
 */
@Tag(name = "Signal Log", description = "자동매매 시그널 이력 조회")
@RestController
@RequestMapping("/api/signals")
public class SignalLogController {

    private final SignalLogRepository repository;

    public SignalLogController(SignalLogRepository repository) {
        this.repository = repository;
    }

    @Operation(summary = "마켓별 최근 시그널 50개 조회")
    @GetMapping
    public ResponseEntity<List<SignalLog>> findByMarket(
            @RequestParam String market) {
        return ResponseEntity.ok(repository.findTop50ByMarketOrderByCreatedAtDesc(market));
    }
}
