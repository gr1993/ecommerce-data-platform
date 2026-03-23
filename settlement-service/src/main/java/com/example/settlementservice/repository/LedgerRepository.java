package com.example.settlementservice.repository;

import com.example.settlementservice.domain.entity.Ledger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface LedgerRepository extends JpaRepository<Ledger, Long> {

    /**
     * 주문 번호와 타입으로 원장 조회 (Upsert 용)
     */
    Optional<Ledger> findByOrderNumberAndLedgerType(String orderNumber, String ledgerType);

    /**
     * 특정 타입과 기간에 대한 합계 금액 및 건수 집계
     */
    @Query("SELECT SUM(l.amount), COUNT(l) FROM Ledger l " +
           "WHERE l.ledgerType = :type AND l.eventAt BETWEEN :start AND :end")
    Object aggregateByTypeAndDate(@Param("type") String type, 
                                  @Param("start") LocalDateTime start, 
                                  @Param("end") LocalDateTime end);
}
