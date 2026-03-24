package com.example.settlementservice.repository;

import com.example.settlementservice.domain.entity.RawOrderCancel;
import com.example.settlementservice.domain.entity.RawOrderCancelId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.settlementservice.domain.entity.ReconciliationStatus;
import java.util.List;

@Repository
public interface RawOrderCancelRepository extends JpaRepository<RawOrderCancel, RawOrderCancelId> {
    List<RawOrderCancel> findByReconciliationStatusIn(List<ReconciliationStatus> statuses);
}
