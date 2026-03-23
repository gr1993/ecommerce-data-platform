package com.example.settlementservice.repository;

import com.example.settlementservice.domain.entity.WeeklyGeneralLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface WeeklyGeneralLedgerRepository extends JpaRepository<WeeklyGeneralLedger, Long> {
    Optional<WeeklyGeneralLedger> findByStartDateAndLedgerType(LocalDate startDate, String ledgerType);
}
