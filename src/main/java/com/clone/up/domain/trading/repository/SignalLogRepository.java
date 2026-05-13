package com.clone.up.domain.trading.repository;

import com.clone.up.domain.trading.entity.SignalLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SignalLogRepository extends JpaRepository<SignalLog, Long> {

    List<SignalLog> findTop50ByMarketOrderByCreatedAtDesc(String market);
}
