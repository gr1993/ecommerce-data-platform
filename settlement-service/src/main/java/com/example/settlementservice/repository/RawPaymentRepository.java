package com.example.settlementservice.repository;

import com.example.settlementservice.domain.entity.RawPayment;
import com.example.settlementservice.domain.entity.RawPaymentId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RawPaymentRepository extends JpaRepository<RawPayment, RawPaymentId> {
}
