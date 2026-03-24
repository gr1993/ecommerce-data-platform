package com.example.settlementservice.repository;

import com.example.settlementservice.domain.entity.RawOrder;
import com.example.settlementservice.domain.entity.RawOrderId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.settlementservice.domain.entity.ReconciliationStatus;
import java.util.List;

@Repository
public interface RawOrderRepository extends JpaRepository<RawOrder, RawOrderId> {
    List<RawOrder> findByReconciliationStatusIn(List<ReconciliationStatus> statuses);
}
