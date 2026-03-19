package com.example.settlementservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartitionManagementService {

    private final JdbcTemplate jdbcTemplate;
    private static final DateTimeFormatter TABLE_NAME_FORMAT = DateTimeFormatter.ofPattern("yyyy_MM_dd");
    private static final DateTimeFormatter SQL_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // 파티션을 생성할 대상 테이블 리스트
    private static final List<String> TARGET_TABLES = List.of(
            "raw_orders",
            "raw_payments",
            "raw_order_cancels",
            "raw_payment_cancels"
    );

    /**
     * 지정된 기준일로부터 N일치의 파티션을 생성합니다.
     * @param baseDate 기준일 (null일 경우 오늘 날짜 사용)
     * @param partitionCount 생성할 파티션 일수
     */
    @Transactional
    public void createFuturePartitions(LocalDate baseDate, int partitionCount) {
        LocalDate start = (baseDate != null) ? baseDate : LocalDate.now();
        
        log.info("[파티션 관리] 생성 시작: 기준일={}, 생성일수={}", start, partitionCount);

        for (int i = 0; i < partitionCount; i++) {
            LocalDate targetDate = start.plusDays(i);
            for (String tableName : TARGET_TABLES) {
                createPartitionIfNotExists(tableName, targetDate);
            }
        }
        
        log.info("[파티션 관리] 모든 파티션 생성 프로세스 완료.");
    }

    private void createPartitionIfNotExists(String parentTable, LocalDate date) {
        String partitionName = String.format("%s_%s", parentTable, date.format(TABLE_NAME_FORMAT));
        String startDate = date.format(SQL_DATE_FORMAT);
        String endDate = date.plusDays(1).format(SQL_DATE_FORMAT);

        if (isPartitionExists(partitionName)) {
            log.debug("[파티션 관리] 파티션이 이미 존재합니다: {}", partitionName);
            return;
        }

        String sql = String.format(
                "CREATE TABLE %s PARTITION OF %s FOR VALUES FROM ('%s 00:00:00') TO ('%s 00:00:00')",
                partitionName, parentTable, startDate, endDate
        );

        try {
            jdbcTemplate.execute(sql);
            log.info("[파티션 관리] 새 파티션 생성 성공: {}", partitionName);
        } catch (Exception e) {
            log.error("[파티션 관리] 파티션 생성 실패: {}. 이유: {}", partitionName, e.getMessage());
        }
    }

    private boolean isPartitionExists(String partitionName) {
        String sql = "SELECT COUNT(*) FROM pg_class c " +
                     "JOIN pg_namespace n ON n.oid = c.relnamespace " +
                     "WHERE c.relname = ? AND n.nspname = 'public'";
        
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, partitionName.toLowerCase());
        return count != null && count > 0;
    }
}
