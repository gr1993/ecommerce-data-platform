package com.example.settlementservice.repository;

import com.example.settlementservice.domain.entity.MonthlyGeneralLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface MonthlyGeneralLedgerRepository extends JpaRepository<MonthlyGeneralLedger, Long> {
    Optional<MonthlyGeneralLedger> findByStartDateAndLedgerType(LocalDate startDate, String ledgerType);
}
