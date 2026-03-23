package com.example.settlementservice.config;

import com.example.settlementservice.domain.entity.WeeklyGeneralLedger;
import com.example.settlementservice.repository.DailyGeneralLedgerRepository;
import com.example.settlementservice.repository.WeeklyGeneralLedgerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * 주간 정산 배치 작업 설정 (Weekly Settlement Job Configuration)
 *
 * [동작 방식]
 * 1. targetDate를 기준으로 '이번 주'와 '전주'의 날짜 범위를 계산합니다.
 * 2. daily_general_ledger 테이블에서 해당 기간의 데이터를 타입별(SALES, CANCEL)로 합산합니다.
 * 3. 집계된 결과를 weekly_general_ledger 테이블에 Upsert 합니다.
 * 
 * [특징]
 * - 매일 03:00 실행 시 이번 주의 누적 수치와 지난 주의 최종 수치를 최신화합니다.
 * - 이미 집계된 일단위 데이터를 사용하므로 성능이 매우 우수합니다.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WeeklySettlementJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DailyGeneralLedgerRepository dailyGeneralLedgerRepository;
    private final WeeklyGeneralLedgerRepository weeklyGeneralLedgerRepository;

    @Bean
    public Job weeklySettlementJob() {
        return new JobBuilder("weeklySettlementJob", jobRepository)
                .start(weeklyAggregationStep())
                .build();
    }

    @Bean
    public Step weeklyAggregationStep() {
        return new StepBuilder("weeklyAggregationStep", jobRepository)
                .tasklet(weeklyAggregationTasklet(null), transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public Tasklet weeklyAggregationTasklet(@Value("#{jobParameters['targetDate']}") String targetDate) {
        return (contribution, chunkContext) -> {
            LocalDate date = LocalDate.parse(targetDate);

            // 1. 이번 주 범위 (월요일 ~ 일요일)
            LocalDate currentWeekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate currentWeekEnd = currentWeekStart.plusDays(6);

            // 2. 전주 범위 (전주 월요일 ~ 전주 일요일)
            LocalDate lastWeekStart = currentWeekStart.minusWeeks(1);
            LocalDate lastWeekEnd = lastWeekStart.plusDays(6);

            log.info("[WeeklyAggregation] 집계 실행 - 기준일: {}, 이번주: {}~{}, 전주: {}~{}", 
                    targetDate, currentWeekStart, currentWeekEnd, lastWeekStart, lastWeekEnd);

            // 전주 및 이번주에 대해 각각 SALES, CANCEL 집계 수행
            aggregateAndSaveWeekly(lastWeekStart, lastWeekEnd, "전주 집계");
            aggregateAndSaveWeekly(currentWeekStart, currentWeekEnd, "이번주 집계(누적)");

            return RepeatStatus.FINISHED;
        };
    }

    private void aggregateAndSaveWeekly(LocalDate start, LocalDate end, String desc) {
        // SALES 집계 및 저장
        processType(start, end, "SALES", desc);
        // CANCEL 집계 및 저장
        processType(start, end, "CANCEL", desc);
    }

    private void processType(LocalDate start, LocalDate end, String type, String desc) {
        Object resultObj = dailyGeneralLedgerRepository.aggregateByPeriodAndType(type, start, end);
        Object[] result = (Object[]) resultObj;

        BigDecimal totalAmount = (result[0] != null) ? (BigDecimal) result[0] : BigDecimal.ZERO;
        int totalCount = (result[1] != null) ? ((Long) result[1]).intValue() : 0;

        WeeklyGeneralLedger weeklyLedger = weeklyGeneralLedgerRepository
                .findByStartDateAndLedgerType(start, type)
                .orElseGet(() -> WeeklyGeneralLedger.builder()
                        .startDate(start)
                        .endDate(end)
                        .ledgerType(type)
                        .build());

        weeklyLedger.setTotalAmount(totalAmount);
        weeklyLedger.setTotalCount(totalCount);
        weeklyLedger.setDescription(desc);

        weeklyGeneralLedgerRepository.save(weeklyLedger);
        log.info("[WeeklyAggregation] {} 완료 - {} ~ {}, 타입: {}, 금액: {}", desc, start, end, type, totalAmount);
    }
}
