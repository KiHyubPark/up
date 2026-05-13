package com.clone.up.domain.trading.repository;

import com.clone.up.domain.trading.entity.DailyTradingRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyTradingRecordRepository extends JpaRepository<DailyTradingRecord, Long> {

    Optional<DailyTradingRecord> findByTradeDate(LocalDate date);
}
