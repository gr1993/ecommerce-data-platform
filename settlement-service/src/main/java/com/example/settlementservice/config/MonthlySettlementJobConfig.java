package com.example.settlementservice.config;

import com.example.settlementservice.domain.entity.MonthlyGeneralLedger;
import com.example.settlementservice.repository.DailyGeneralLedgerRepository;
import com.example.settlementservice.repository.MonthlyGeneralLedgerRepository;
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
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * 월간 정산 배치 작업 설정 (Monthly Settlement Job Configuration)
 *
 * [동작 방식]
 * 1. targetDate를 기준으로 '이번 달'의 범위를 계산합니다.
 * 2. [7일 윈도우 반영] 만약 오늘이 월초(1일~7일)라면, 전월 지연 데이터 처리를 위해 '지난 달' 데이터도 함께 집계합니다.
 * 3. daily_general_ledger 테이블에서 해당 기간 데이터를 타입별로 합산하여 monthly_general_ledger에 Upsert 합니다.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MonthlySettlementJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DailyGeneralLedgerRepository dailyGeneralLedgerRepository;
    private final MonthlyGeneralLedgerRepository monthlyGeneralLedgerRepository;

    @Bean
    public Job monthlySettlementJob() {
        return new JobBuilder("monthlySettlementJob", jobRepository)
                .start(monthlyAggregationStep())
                .build();
    }

    @Bean
    public Step monthlyAggregationStep() {
        return new StepBuilder("monthlyAggregationStep", jobRepository)
                .tasklet(monthlyAggregationTasklet(null), transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public Tasklet monthlyAggregationTasklet(@Value("#{jobParameters['targetDate']}") String targetDate) {
        return (contribution, chunkContext) -> {
            LocalDate date = LocalDate.parse(targetDate);

            // 1. 이번 달 범위
            LocalDate currentMonthStart = date.with(TemporalAdjusters.firstDayOfMonth());
            LocalDate currentMonthEnd = date.with(TemporalAdjusters.lastDayOfMonth());

            log.info("[MonthlyAggregation] 이번달 집계 - {} ~ {}", currentMonthStart, currentMonthEnd);
            aggregateAndSaveMonthly(currentMonthStart, currentMonthEnd, "이번달 집계(누적)");

            // 2. 월초(1~7일)인 경우 지난 달도 재집계 (7일 윈도우 대응)
            if (date.getDayOfMonth() <= 7) {
                LocalDate lastMonthStart = currentMonthStart.minusMonths(1);
                LocalDate lastMonthEnd = lastMonthStart.with(TemporalAdjusters.lastDayOfMonth());
                
                log.info("[MonthlyAggregation] 월초 윈도우 적용 - 지난달 재집계: {} ~ {}", lastMonthStart, lastMonthEnd);
                aggregateAndSaveMonthly(lastMonthStart, lastMonthEnd, "지난달 집계(최종)");
            }

            return RepeatStatus.FINISHED;
        };
    }

    private void aggregateAndSaveMonthly(LocalDate start, LocalDate end, String desc) {
        processType(start, end, "SALES", desc);
        processType(start, end, "CANCEL", desc);
    }

    private void processType(LocalDate start, LocalDate end, String type, String desc) {
        Object resultObj = dailyGeneralLedgerRepository.aggregateByPeriodAndType(type, start, end);
        Object[] result = (Object[]) resultObj;

        BigDecimal totalAmount = (result[0] != null) ? (BigDecimal) result[0] : BigDecimal.ZERO;
        int totalCount = (result[1] != null) ? ((Long) result[1]).intValue() : 0;

        MonthlyGeneralLedger monthlyLedger = monthlyGeneralLedgerRepository
                .findByStartDateAndLedgerType(start, type)
                .orElseGet(() -> MonthlyGeneralLedger.builder()
                        .startDate(start)
                        .endDate(end)
                        .ledgerType(type)
                        .build());

        monthlyLedger.setTotalAmount(totalAmount);
        monthlyLedger.setTotalCount(totalCount);
        monthlyLedger.setDescription(desc);

        monthlyGeneralLedgerRepository.save(monthlyLedger);
        log.info("[MonthlyAggregation] {} 완료 - {} ~ {}, 타입: {}, 금액: {}", desc, start, end, type, totalAmount);
    }
}
