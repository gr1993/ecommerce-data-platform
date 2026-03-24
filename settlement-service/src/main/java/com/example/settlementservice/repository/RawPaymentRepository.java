package com.example.settlementservice.repository;

import com.example.settlementservice.domain.entity.RawPayment;
import com.example.settlementservice.domain.entity.RawPaymentId;
import com.example.settlementservice.domain.entity.ReconciliationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RawPaymentRepository extends JpaRepository<RawPayment, RawPaymentId> {
    List<RawPayment> findByOrderNumber(String orderNumber);
    List<RawPayment> findByReconciliationStatusIn(List<ReconciliationStatus> statuses);
}
