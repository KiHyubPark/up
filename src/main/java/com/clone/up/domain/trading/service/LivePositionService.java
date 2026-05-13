package com.clone.up.domain.trading.service;

import com.clone.up.domain.candle.entity.CandleType;
import com.clone.up.domain.strategy.StrategyType;
import com.clone.up.domain.trading.entity.LivePosition;
import com.clone.up.domain.trading.entity.PositionStatus;
import com.clone.up.domain.trading.repository.LivePositionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 자동매매 포지션 영속화 서비스.
 *
 * <p>매수/매도 시 DB에 상태를 저장하여 서버 재시작 후에도 포지션을 복구한다.
 * 동일 (market, strategyType) 조합의 OPEN 포지션은 1개만 허용한다.
 */
@Service
@Transactional
public class LivePositionService {

    private static final Logger log = LoggerFactory.getLogger(LivePositionService.class);

    private final LivePositionRepository repository;

    public LivePositionService(LivePositionRepository repository) {
        this.repository = repository;
    }

    /**
     * 매수 시 포지션을 열고 DB에 저장한다.
     *
     * @throws IllegalStateException 이미 OPEN 포지션이 존재하는 경우 (중복 매수 방지)
     */
    public LivePosition openPosition(
            String market,
            StrategyType strategyType,
            CandleType candleType,
            BigDecimal entryPrice,
            LocalDateTime entryTime,
            BigDecimal entryAtrValue,
            BigDecimal quantity) {

        // 중복 매수 방지
        Optional<LivePosition> existing = findOpenPosition(market, strategyType);
        if (existing.isPresent()) {
            throw new IllegalStateException(
                    "이미 오픈된 포지션이 존재합니다. market=%s, strategy=%s, positionId=%d"
                            .formatted(market, strategyType, existing.get().getId()));
        }

        LivePosition position = LivePosition.open(
                market, strategyType, candleType,
                entryPrice, entryTime, entryAtrValue, quantity);

        LivePosition saved = repository.save(position);
        log.info("포지션 오픈 — id={}, market={}, strategy={}, entryPrice={}, quantity={}",
                saved.getId(), market, strategyType, entryPrice, quantity);
        return saved;
    }

    /**
     * 매도 시 OPEN 포지션을 청산 처리하고 DB에 반영한다.
     *
     * @throws IllegalStateException OPEN 포지션이 없는 경우
     */
    public LivePosition closePosition(
            String market,
            StrategyType strategyType,
            BigDecimal exitPrice,
            LocalDateTime exitTime) {

        LivePosition position = findOpenPosition(market, strategyType)
                .orElseThrow(() -> new IllegalStateException(
                        "청산할 오픈 포지션이 없습니다. market=%s, strategy=%s"
                                .formatted(market, strategyType)));

        position.close(exitPrice, exitTime);

        log.info("포지션 청산 — id={}, market={}, strategy={}, exitPrice={}, pnl={}",
                position.getId(), market, strategyType, exitPrice, position.getRealizedPnl());
        return position;
    }

    /**
     * 특정 마켓+전략의 현재 OPEN 포지션을 반환한다.
     * 서버 재시작 후 자동매매 루프 시작 전에 호출하여 기존 포지션을 복구한다.
     */
    @Transactional(readOnly = true)
    public Optional<LivePosition> findOpenPosition(String market, StrategyType strategyType) {
        return repository.findByMarketAndStrategyTypeAndStatus(market, strategyType, PositionStatus.OPEN);
    }

    /**
     * 모든 OPEN 포지션 목록 — 서버 재시작 시 전체 상태 복구에 사용한다.
     */
    @Transactional(readOnly = true)
    public List<LivePosition> findAllOpenPositions() {
        return repository.findAllByStatus(PositionStatus.OPEN);
    }

    /**
     * 특정 마켓의 전체 포지션 이력 (최신 순).
     */
    @Transactional(readOnly = true)
    public List<LivePosition> findHistory(String market) {
        return repository.findAllByMarketOrderByCreatedAtDesc(market);
    }

    /** ID로 포지션 단건 조회 */
    @Transactional(readOnly = true)
    public Optional<LivePosition> findById(Long id) {
        return repository.findById(id);
    }
}
