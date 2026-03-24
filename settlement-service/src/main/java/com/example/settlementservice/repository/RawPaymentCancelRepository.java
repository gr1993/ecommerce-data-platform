package com.example.settlementservice.repository;

import com.example.settlementservice.domain.entity.RawPaymentCancel;
import com.example.settlementservice.domain.entity.RawPaymentCancelId;
import com.example.settlementservice.domain.entity.ReconciliationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RawPaymentCancelRepository extends JpaRepository<RawPaymentCancel, RawPaymentCancelId> {
    List<RawPaymentCancel> findByOrderNumber(String orderNumber);
    List<RawPaymentCancel> findByReconciliationStatusIn(List<ReconciliationStatus> statuses);
}
