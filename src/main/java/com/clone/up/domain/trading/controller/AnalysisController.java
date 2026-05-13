package com.clone.up.domain.trading.controller;

import com.clone.up.domain.trading.dto.AnalysisSummaryResponse;
import com.clone.up.domain.trading.dto.LivePositionResponse;
import com.clone.up.domain.trading.entity.PositionStatus;
import com.clone.up.domain.trading.repository.LivePositionRepository;
import com.clone.up.domain.trading.service.AnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 자동매매 성과 분석 API.
 *
 * <ul>
 *   <li>GET /api/analysis/summary?market=KRW-BTC — 성과 요약 (승률, 총 손익, MDD 등)</li>
 *   <li>GET /api/analysis/positions?market=KRW-BTC — 청산 포지션 이력</li>
 * </ul>
 */
@Tag(name = "Analysis", description = "자동매매 성과 분석 API")
@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final AnalysisService analysisService;
    private final LivePositionRepository positionRepository;

    public AnalysisController(AnalysisService analysisService, LivePositionRepository positionRepository) {
        this.analysisService = analysisService;
        this.positionRepository = positionRepository;
    }

    @Operation(
            summary = "성과 요약",
            description = "특정 마켓(또는 전체)의 거래 성과를 집계한다. " +
                    "market 파라미터 생략 시 전체 마켓 합산."
    )
    @GetMapping("/summary")
    public AnalysisSummaryResponse summary(
            @RequestParam(required = false) String market) {
        return analysisService.summarize(market);
    }

    @Operation(
            summary = "포지션 이력 조회",
            description = "마켓별 포지션 이력을 반환한다. " +
                    "status 파라미터로 OPEN/CLOSED 필터링 가능 (기본: CLOSED)."
    )
    @GetMapping("/positions")
    public List<LivePositionResponse> positions(
            @RequestParam String market,
            @RequestParam(defaultValue = "CLOSED") PositionStatus status) {
        return positionRepository.findAllByMarketAndStatusOrderByExitTimeAsc(market, status)
                .stream()
                .map(LivePositionResponse::from)
                .toList();
    }
}
