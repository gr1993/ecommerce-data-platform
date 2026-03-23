package com.example.settlementservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
public class SettlementSchedulerService {

    private final JobLauncher jobLauncher;
    private final Job dailySettlementJob;
    private final Job weeklySettlementJob;
    private final Job monthlySettlementJob;

    public SettlementSchedulerService(
            JobLauncher jobLauncher,
            @Qualifier("dailySettlementJob") Job dailySettlementJob,
            @Qualifier("weeklySettlementJob") Job weeklySettlementJob,
            @Qualifier("monthlySettlementJob") Job monthlySettlementJob) {
        this.jobLauncher = jobLauncher;
        this.dailySettlementJob = dailySettlementJob;
        this.weeklySettlementJob = weeklySettlementJob;
        this.monthlySettlementJob = monthlySettlementJob;
    }

    /**
     * 일일 정산 배치 실행 (매일 새벽 02:00)
     * 대상 날짜: 어제 (LocalDate.now().minusDays(1))
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void runDailyJob() {
        String targetDate = LocalDate.now().minusDays(1).toString();
        log.info("[Scheduler] 일일 정산 시작 - TargetDate: {}", targetDate);
        executeJob(dailySettlementJob, targetDate);
    }

    /**
     * 주간 및 월간 정산 배치 실행 (매일 새벽 03:00)
     * 일일 배치가 완료된 후의 최신 데이터로 주/월간 수치를 갱신합니다.
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void runWeeklyAndMonthlyJobs() {
        String targetDate = LocalDate.now().minusDays(1).toString();
        log.info("[Scheduler] 주간/월간 정산 시작 - TargetDate: {}", targetDate);
        
        // 두 Job을 순차적으로 실행
        executeJob(weeklySettlementJob, targetDate);
        executeJob(monthlySettlementJob, targetDate);
    }

    private void executeJob(Job job, String targetDate) {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addString("targetDate", targetDate)
                    .addString("datetime", LocalDateTime.now().toString()) // 고유성 보장
                    .toJobParameters();
            
            jobLauncher.run(job, params);
        } catch (Exception e) {
            log.error("[Scheduler] Job 실행 중 오류 발생: {} - {}", job.getName(), e.getMessage());
        }
    }
}
