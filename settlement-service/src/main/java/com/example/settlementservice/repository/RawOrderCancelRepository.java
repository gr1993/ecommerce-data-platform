package com.example.settlementservice.repository;

import com.example.settlementservice.domain.entity.RawOrderCancel;
import com.example.settlementservice.domain.entity.RawOrderCancelId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RawOrderCancelRepository extends JpaRepository<RawOrderCancel, RawOrderCancelId> {
}
