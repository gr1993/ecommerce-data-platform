package com.example.settlementservice.repository;

import com.example.settlementservice.domain.entity.RawPaymentCancel;
import com.example.settlementservice.domain.entity.RawPaymentCancelId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RawPaymentCancelRepository extends JpaRepository<RawPaymentCancel, RawPaymentCancelId> {
}
