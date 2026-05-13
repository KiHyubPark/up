package com.clone.up.domain.trading.repository;

import com.clone.up.domain.strategy.StrategyType;
import com.clone.up.domain.trading.entity.LivePosition;
import com.clone.up.domain.trading.entity.PositionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LivePositionRepository extends JpaRepository<LivePosition, Long> {

    /** 특정 마켓+전략의 현재 오픈 포지션 (최대 1개) */
    Optional<LivePosition> findByMarketAndStrategyTypeAndStatus(
            String market, StrategyType strategyType, PositionStatus status);

    /** 모든 오픈 포지션 — 서버 재시작 시 상태 복구에 사용 */
    List<LivePosition> findAllByStatus(PositionStatus status);

    /** 특정 마켓의 전체 포지션 이력 */
    List<LivePosition> findAllByMarketOrderByCreatedAtDesc(String market);
}
