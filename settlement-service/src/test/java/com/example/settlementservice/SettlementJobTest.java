package com.example.settlementservice;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBatchTest
@SpringBootTest
public class SettlementJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    @Qualifier("dailySettlementJob")
    private Job dailySettlementJob;

    @Autowired
    @Qualifier("weeklySettlementJob")
    private Job weeklySettlementJob;

    @Autowired
    @Qualifier("monthlySettlementJob")
    private Job monthlySettlementJob;

    @Test
    public void testDailySettlementJob() throws Exception {
        // Given
        String targetDate = "2026-03-23";

        /*
         * JobParameter에 targetDate만 넘기는 이유는 작업 실패 시 동일한 파라미터로 재시작하여
         * 실패한 지점부터 다시 실행(Restart)할 수 있도록 하기 위함입니다. (Spring Batch의 기본 동작)
         * 만약 이미 성공한 날짜에 대해 강제로 새 Job 인스턴스를 실행하고 싶다면,
         * 'version'이나 'timestamp'와 같은 식별용 파라미터를 추가하여 실행하면 됩니다.
         */
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("targetDate", targetDate)
                .toJobParameters();

        // When
        jobLauncherTestUtils.setJob(dailySettlementJob);
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // Then
        System.out.println("Daily Job Execution Status: " + jobExecution.getStatus());
        assertEquals(ExitStatus.COMPLETED.getExitCode(), jobExecution.getExitStatus().getExitCode());
    }

    @Test
    public void testWeeklySettlementJob() throws Exception {
        // Given
        String targetDate = "2026-03-23";
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("targetDate", targetDate)
                .toJobParameters();

        // When
        jobLauncherTestUtils.setJob(weeklySettlementJob);
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // Then
        System.out.println("Weekly Job Execution Status: " + jobExecution.getStatus());
        assertEquals(ExitStatus.COMPLETED.getExitCode(), jobExecution.getExitStatus().getExitCode());
    }

    @Test
    public void testMonthlySettlementJob() throws Exception {
        // Given
        String targetDate = "2026-03-23";
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("targetDate", targetDate)
                .toJobParameters();

        // When
        jobLauncherTestUtils.setJob(monthlySettlementJob);
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // Then
        System.out.println("Monthly Job Execution Status: " + jobExecution.getStatus());
        assertEquals(ExitStatus.COMPLETED.getExitCode(), jobExecution.getExitStatus().getExitCode());
    }
}
