package com.clone.up.domain.trading.repository;

import com.clone.up.domain.strategy.StrategyType;
import com.clone.up.domain.trading.entity.LivePosition;
import com.clone.up.domain.trading.entity.PositionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LivePositionRepository extends JpaRepository<LivePosition, Long> {

    /** 특정 마켓+전략의 현재 오픈 포지션 (최대 1개) */
    Optional<LivePosition> findByMarketAndStrategyTypeAndStatus(
            String market, StrategyType strategyType, PositionStatus status);

    /**
     * 오픈 포지션 조회 + 비관적 쓰기 락.
     *
     * <p>다중 인스턴스 환경에서 동시에 포지션을 열려는 경우 DB 레벨에서 방어한다.
     * 단일 인스턴스에서는 {@link com.clone.up.domain.trading.service.OrderGuard}가 1차 방어선이다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM LivePosition p WHERE p.market = :market AND p.strategyType = :strategyType AND p.status = :status")
    Optional<LivePosition> findByMarketAndStrategyTypeAndStatusForUpdate(
            @Param("market") String market,
            @Param("strategyType") StrategyType strategyType,
            @Param("status") PositionStatus status);

    /** 모든 오픈 포지션 — 서버 재시작 시 상태 복구에 사용 */
    List<LivePosition> findAllByStatus(PositionStatus status);

    /** 특정 마켓의 전체 포지션 이력 */
    List<LivePosition> findAllByMarketOrderByCreatedAtDesc(String market);

    /** 특정 마켓의 청산된 포지션 — 청산 시각 오름차순 (MDD·수익 계산용) */
    List<LivePosition> findAllByMarketAndStatusOrderByExitTimeAsc(String market, PositionStatus status);

    /** 전체 마켓 청산 포지션 — 청산 시각 오름차순 */
    List<LivePosition> findAllByStatusOrderByExitTimeAsc(PositionStatus status);
}
