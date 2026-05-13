package com.clone.up.domain.trading.controller;

import com.clone.up.domain.trading.service.EmergencyStopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 긴급 중단 스위치 REST API.
 *
 * <ul>
 *   <li>POST  /api/trading/emergency-stop           — 긴급 중단 활성화</li>
 *   <li>DELETE /api/trading/emergency-stop          — 긴급 중단 해제</li>
 *   <li>GET   /api/trading/emergency-stop           — 현재 상태 조회</li>
 * </ul>
 */
@Tag(name = "Emergency Stop", description = "자동매매 긴급 중단 스위치")
@RestController
@RequestMapping("/api/trading/emergency-stop")
public class EmergencyStopController {

    private final EmergencyStopService service;

    public EmergencyStopController(EmergencyStopService service) {
        this.service = service;
    }

    @Operation(summary = "긴급 중단 활성화")
    @PostMapping
    public ResponseEntity<Map<String, Object>> activate(
            @RequestParam(defaultValue = "수동 긴급 중단") String reason) {
        service.activate(reason);
        return ResponseEntity.ok(Map.of(
                "stopped", true,
                "reason", reason
        ));
    }

    @Operation(summary = "긴급 중단 해제")
    @DeleteMapping
    public ResponseEntity<Map<String, Object>> deactivate() {
        service.deactivate();
        return ResponseEntity.ok(Map.of("stopped", false));
    }

    @Operation(summary = "긴급 중단 상태 조회")
    @GetMapping
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of("stopped", service.isStopped()));
    }
}
