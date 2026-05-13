package com.clone.up.domain.trading.dto;

import java.math.BigDecimal;

/**
 * 자동매매 성과 요약 응답 DTO.
 *
 * <ul>
 *   <li>totalTrades: 총 청산 거래 수</li>
 *   <li>winTrades: 수익 거래 수 (realizedPnl > 0)</li>
 *   <li>lossTrades: 손실 거래 수 (realizedPnl ≤ 0)</li>
 *   <li>winRate: 승률 (%)</li>
 *   <li>totalPnl: 총 손익 (원)</li>
 *   <li>avgPnl: 거래당 평균 손익 (원)</li>
 *   <li>avgPnlPercent: 거래당 평균 수익률 (%)</li>
 *   <li>maxDrawdown: 최대 낙폭 MDD — 누적 손익 고점 대비 최대 하락 (원)</li>
 *   <li>maxDrawdownPercent: MDD (%) — 고점 대비 비율</li>
 *   <li>openPositionCount: 현재 오픈 포지션 수</li>
 * </ul>
 */
public record AnalysisSummaryResponse(
        String market,
        int totalTrades,
        int winTrades,
        int lossTrades,
        BigDecimal winRate,
        BigDecimal totalPnl,
        BigDecimal avgPnl,
        BigDecimal avgPnlPercent,
        BigDecimal maxDrawdown,
        BigDecimal maxDrawdownPercent,
        int openPositionCount
) {
}
