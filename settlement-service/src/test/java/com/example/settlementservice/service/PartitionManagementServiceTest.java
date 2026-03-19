package com.example.settlementservice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

@SpringBootTest
@ActiveProfiles("dev") // 필요 시 프로파일 설정
class PartitionManagementServiceTest {

    @Autowired
    private PartitionManagementService partitionManagementService;

    @Test
    @DisplayName("오늘부터 향후 7일간의 파티션을 생성한다.")
    void createNext7DaysPartitions() {
        // 기준일: 오늘, 생성 일수: 7일
        partitionManagementService.createFuturePartitions(LocalDate.now(), 7);
    }

    @Test
    @DisplayName("특정 날짜로부터 지정된 개수만큼 파티션을 생성한다.")
    void createCustomPartitions() {
        // 기준일: 2026-04-01, 생성 일수: 5일
        LocalDate baseDate = LocalDate.of(2026, 4, 1);
        partitionManagementService.createFuturePartitions(baseDate, 5);
    }
}
