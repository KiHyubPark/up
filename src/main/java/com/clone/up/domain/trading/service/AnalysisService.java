package com.clone.up.domain.trading.service;

import com.clone.up.domain.trading.dto.AnalysisSummaryResponse;
import com.clone.up.domain.trading.entity.LivePosition;
import com.clone.up.domain.trading.entity.PositionStatus;
import com.clone.up.domain.trading.repository.LivePositionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

@Service
public class AnalysisService {

    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);
    private static final int SCALE = 4;

    private final LivePositionRepository positionRepository;

    public AnalysisService(LivePositionRepository positionRepository) {
        this.positionRepository = positionRepository;
    }

    /**
     * 특정 마켓의 성과 요약을 계산한다.
     * market이 null이면 전체 마켓을 집계한다.
     */
    public AnalysisSummaryResponse summarize(String market) {
        List<LivePosition> closed = market == null
                ? positionRepository.findAllByStatusOrderByExitTimeAsc(PositionStatus.CLOSED)
                : positionRepository.findAllByMarketAndStatusOrderByExitTimeAsc(market, PositionStatus.CLOSED);

        int totalTrades = closed.size();
        int winTrades = 0;
        int lossTrades = 0;
        BigDecimal totalPnl = BigDecimal.ZERO;
        BigDecimal totalPnlPercent = BigDecimal.ZERO;

        BigDecimal cumulativePnl = BigDecimal.ZERO;
        BigDecimal peak = BigDecimal.ZERO;
        BigDecimal maxDrawdown = BigDecimal.ZERO;
        BigDecimal peakForPercent = BigDecimal.ZERO;
        BigDecimal maxDrawdownPercent = BigDecimal.ZERO;

        for (LivePosition p : closed) {
            BigDecimal pnl = p.getRealizedPnl() != null ? p.getRealizedPnl() : BigDecimal.ZERO;
            if (pnl.compareTo(BigDecimal.ZERO) > 0) winTrades++;
            else lossTrades++;

            totalPnl = totalPnl.add(pnl);

            // 수익률 = pnl / investedAmount * 100
            if (p.getInvestedAmount() != null && p.getInvestedAmount().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal pnlPct = pnl.divide(p.getInvestedAmount(), MC).multiply(BigDecimal.valueOf(100));
                totalPnlPercent = totalPnlPercent.add(pnlPct);
            }

            // MDD 계산: 누적 손익 기준
            cumulativePnl = cumulativePnl.add(pnl);
            if (cumulativePnl.compareTo(peak) > 0) {
                peak = cumulativePnl;
                peakForPercent = peak;
            }
            BigDecimal drawdown = peak.subtract(cumulativePnl);
            if (drawdown.compareTo(maxDrawdown) > 0) {
                maxDrawdown = drawdown;
                // MDD% = drawdown / peak × 100 (peak이 0보다 클 때만)
                if (peakForPercent.compareTo(BigDecimal.ZERO) > 0) {
                    maxDrawdownPercent = drawdown.divide(peakForPercent, MC).multiply(BigDecimal.valueOf(100));
                }
            }
        }

        BigDecimal winRate = totalTrades == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(winTrades)
                        .divide(BigDecimal.valueOf(totalTrades), MC)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(SCALE, RoundingMode.HALF_UP);

        BigDecimal avgPnl = totalTrades == 0 ? BigDecimal.ZERO
                : totalPnl.divide(BigDecimal.valueOf(totalTrades), MC).setScale(SCALE, RoundingMode.HALF_UP);

        BigDecimal avgPnlPercent = totalTrades == 0 ? BigDecimal.ZERO
                : totalPnlPercent.divide(BigDecimal.valueOf(totalTrades), MC).setScale(SCALE, RoundingMode.HALF_UP);

        int openCount = market == null
                ? positionRepository.findAllByStatus(PositionStatus.OPEN).size()
                : (int) positionRepository.findAllByStatus(PositionStatus.OPEN).stream()
                        .filter(p -> p.getMarket().equals(market))
                        .count();

        return new AnalysisSummaryResponse(
                market != null ? market : "ALL",
                totalTrades,
                winTrades,
                lossTrades,
                winRate,
                totalPnl.setScale(SCALE, RoundingMode.HALF_UP),
                avgPnl,
                avgPnlPercent,
                maxDrawdown.setScale(SCALE, RoundingMode.HALF_UP),
                maxDrawdownPercent.setScale(SCALE, RoundingMode.HALF_UP),
                openCount
        );
    }
}
