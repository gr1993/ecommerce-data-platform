package com.example.settlementservice.repository;

import com.example.settlementservice.domain.entity.DailyGeneralLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyGeneralLedgerRepository extends JpaRepository<DailyGeneralLedger, Long> {
    
    /**
     * 특정 날짜와 타입(매출/취소)으로 원장 조회
     */
    Optional<DailyGeneralLedger> findBySettlementDateAndLedgerType(LocalDate settlementDate, String ledgerType);
}
