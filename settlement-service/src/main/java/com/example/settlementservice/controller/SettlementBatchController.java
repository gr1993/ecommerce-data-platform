package com.example.settlementservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/settlement")
public class SettlementBatchController {

    private final JobLauncher jobLauncher;
    private final Job dailySettlementJob;
    private final Job weeklySettlementJob;

    public SettlementBatchController(
            JobLauncher jobLauncher, 
            @Qualifier("dailySettlementJob") Job dailySettlementJob,
            @Qualifier("weeklySettlementJob") Job weeklySettlementJob) {
        this.jobLauncher = jobLauncher;
        this.dailySettlementJob = dailySettlementJob;
        this.weeklySettlementJob = weeklySettlementJob;
    }

    @PostMapping("/daily/run")
    public String runDailySettlementJob(@RequestParam(name = "targetDate") String targetDate) throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("targetDate", targetDate)
                .addString("datetime", LocalDateTime.now().toString())
                .toJobParameters();
        
        jobLauncher.run(dailySettlementJob, jobParameters);
        return "Daily Settlement Batch Job started for date: " + targetDate;
    }

    @PostMapping("/weekly/run")
    public String runWeeklySettlementJob(@RequestParam(name = "targetDate") String targetDate) throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("targetDate", targetDate)
                .addString("datetime", LocalDateTime.now().toString())
                .toJobParameters();
        
        jobLauncher.run(weeklySettlementJob, jobParameters);
        return "Weekly Settlement Batch Job started for date: " + targetDate;
    }
}
