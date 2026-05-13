package com.clone.up.domain.trading.service;

import com.clone.up.config.TradingProperties;
import com.clone.up.domain.strategy.StrategyFactory;
import com.clone.up.domain.trading.entity.DailyTradingRecord;
import com.clone.up.domain.trading.entity.LivePosition;
import com.clone.up.domain.trading.entity.SignalLog;
import com.clone.up.domain.trading.entity.SignalType;
import com.clone.up.domain.trading.repository.SignalLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 자동매매 실행 오케스트레이터.
 *
 * <p>스케줄러가 이 서비스를 호출하면 다음 순서로 처리한다:
 * <ol>
 *   <li>긴급 중단 여부 확인 ({@link EmergencyStopService})</li>
 *   <li>일일 손실 한도 확인 ({@link DailyRiskGuard})</li>
 *   <li>현재 오픈 포지션 조회</li>
 *   <li>포지션 없음 → 진입 시그널 평가 → 매수 실행</li>
 *   <li>포지션 있음 → 청산 시그널 평가 → 매도 실행</li>
 *   <li>시그널 기록 ({@link SignalLog})</li>
 * </ol>
 *
 * <p>PAPER/LIVE 모두 LivePosition을 DB에 영속화한다.
 * PAPER 모드에서는 실제 주문 API를 호출하지 않는다 (시뮬레이션).
 * LIVE 모드에서는 실제 주문 API를 추가로 호출한다 (미구현, 추후 연동).
 */
@Service
public class TradingExecutionService {

    private static final Logger log = LoggerFactory.getLogger(TradingExecutionService.class);

    private final TradingProperties properties;
    private final StrategyFactory strategyFactory;
    private final EmergencyStopService emergencyStop;
    private final DailyRiskGuard dailyRiskGuard;
    private final OrderGuard orderGuard;
    private final LivePositionService positionService;
    private final TradingSignalEvaluator signalEvaluator;
    private final SignalLogRepository signalLogRepository;

    public TradingExecutionService(
            TradingProperties properties,
            StrategyFactory strategyFactory,
            EmergencyStopService emergencyStop,
            DailyRiskGuard dailyRiskGuard,
            OrderGuard orderGuard,
            LivePositionService positionService,
            TradingSignalEvaluator signalEvaluator,
            SignalLogRepository signalLogRepository) {
        this.properties = properties;
        this.strategyFactory = strategyFactory;
        this.emergencyStop = emergencyStop;
        this.dailyRiskGuard = dailyRiskGuard;
        this.orderGuard = orderGuard;
        this.positionService = positionService;
        this.signalEvaluator = signalEvaluator;
        this.signalLogRepository = signalLogRepository;
    }

    /**
     * 매매 사이클을 1회 실행한다.
     * 스케줄러에서 캔들 마감 직후 호출된다.
     */
    public void execute() {
        // 1. 긴급 중단 확인
        if (emergencyStop.isStopped()) {
            log.info("긴급 중단 활성 — 매매 사이클 스킵");
            return;
        }

        // 2. 일일 손실 한도 확인
        if (!dailyRiskGuard.isTradeAllowed()) {
            log.info("일일 손실 한도 초과 — 매매 사이클 스킵");
            return;
        }

        // 3. 현재 오픈 포지션 확인
        Optional<LivePosition> openPosition = positionService.findOpenPosition(
                properties.getMarket(), strategyFactory.strategyType());

        if (openPosition.isPresent()) {
            evaluateExit(openPosition.get());
        } else {
            evaluateEntry();
        }
    }

    // ── Entry ────────────────────────────────────────────────────────────────

    private void evaluateEntry() {
        boolean signal = signalEvaluator.isEntrySignal();
        BigDecimal price = signalEvaluator.lastClosePrice();

        if (!signal) {
            log.debug("진입 시그널 없음 — market={}", properties.getMarket());
            return;
        }

        log.info("진입 시그널 발생 — market={}, strategy={}, price={}, mode={}",
                properties.getMarket(), strategyFactory.strategyType(), price, properties.getMode());

        executeEntry(price);
    }

    @Transactional
    protected void executeEntry(BigDecimal price) {
        try (var lock = orderGuard.acquire(properties.getMarket(), strategyFactory.strategyType())) {
            BigDecimal quantity = calculateQuantity(price);

            LivePosition opened = positionService.openPosition(
                    properties.getMarket(),
                    strategyFactory.strategyType(),
                    strategyFactory.candleType(),
                    properties.getMode(),
                    price,
                    LocalDateTime.now(),
                    BigDecimal.ZERO,  // ATR값: 실제 API 연결 후 계산값으로 대체
                    quantity
            );
            saveSignalLog(SignalType.BUY_SIGNAL, price,
                    "positionId=" + opened.getId() + ",mode=" + properties.getMode());

            if (properties.getMode().isLive()) {
                // 실제 주문 API 호출 (미구현 — 추후 연동)
                log.info("LIVE 주문 API 호출 예정 — positionId={}", opened.getId());
            }
        } catch (OrderGuard.DuplicateOrderException e) {
            log.warn("중복 진입 차단 — {}", e.getMessage());
        }
    }

    // ── Exit ─────────────────────────────────────────────────────────────────

    private void evaluateExit(LivePosition position) {
        boolean signal = signalEvaluator.isExitSignal(position);
        BigDecimal price = signalEvaluator.lastClosePrice();

        if (!signal) {
            log.debug("청산 시그널 없음 — market={}, positionId={}",
                    properties.getMarket(), position.getId());
            return;
        }

        log.info("청산 시그널 발생 — market={}, strategy={}, positionId={}, price={}, mode={}",
                properties.getMarket(), strategyFactory.strategyType(), position.getId(), price,
                properties.getMode());

        executeExit(position, price);
    }

    @Transactional
    protected void executeExit(LivePosition position, BigDecimal price) {
        LivePosition closed = positionService.closePosition(
                properties.getMarket(),
                strategyFactory.strategyType(),
                price,
                LocalDateTime.now()
        );
        saveSignalLog(SignalType.SELL_SIGNAL, price,
                "positionId=" + closed.getId() + ",pnl=" + closed.getRealizedPnl()
                        + ",mode=" + properties.getMode());
        dailyRiskGuard.recordClose(closed.getRealizedPnl());

        if (properties.getMode().isLive()) {
            // 실제 주문 API 호출 (미구현 — 추후 연동)
            log.info("LIVE 매도 주문 API 호출 예정 — positionId={}", closed.getId());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private BigDecimal calculateQuantity(BigDecimal price) {
        if (price.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        BigDecimal investAmount = properties.getInitialCapital()
                .multiply(BigDecimal.valueOf(properties.getInvestRatio()));
        return investAmount.divide(price, 8, java.math.RoundingMode.DOWN);
    }

    @Transactional
    protected void saveSignalLog(SignalType signalType, BigDecimal price, String note) {
        SignalLog sigLog = SignalLog.of(
                properties.getMarket(),
                strategyFactory.strategyType(),
                strategyFactory.candleType(),
                signalType,
                properties.getMode(),
                price,
                note
        );
        signalLogRepository.save(sigLog);
    }
}
