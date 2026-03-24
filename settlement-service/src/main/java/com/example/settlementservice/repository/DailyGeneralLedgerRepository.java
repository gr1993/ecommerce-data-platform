package com.example.settlementservice.repository;

import com.example.settlementservice.domain.entity.DailyGeneralLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyGeneralLedgerRepository extends JpaRepository<DailyGeneralLedger, Long> {
    
    /**
     * 특정 날짜와 타입(매출/취소)으로 원장 조회
     */
    Optional<DailyGeneralLedger> findBySettlementDateAndLedgerType(LocalDate settlementDate, String ledgerType);

    /**
     * 기간별 정산 내역 리스트 조회
     */
    List<DailyGeneralLedger> findBySettlementDateBetweenOrderBySettlementDateAsc(LocalDate start, LocalDate end);

    /**
     * 기간별 타입별 합계 집계
     */
    @Query("SELECT SUM(d.totalAmount), SUM(d.totalCount) FROM DailyGeneralLedger d " +
           "WHERE d.ledgerType = :type AND d.settlementDate BETWEEN :start AND :end")
    Object aggregateByPeriodAndType(@Param("type") String type, 
                                    @Param("start") LocalDate start, 
                                    @Param("end") LocalDate end);
}
