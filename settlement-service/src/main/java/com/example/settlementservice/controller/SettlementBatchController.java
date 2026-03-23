package com.example.settlementservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/settlement")
@RequiredArgsConstructor
public class SettlementBatchController {

    private final JobLauncher jobLauncher;
    private final Job settlementJob;

    @PostMapping("/run")
    public String runSettlementJob() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("datetime", LocalDateTime.now().toString())
                .toJobParameters();
        
        jobLauncher.run(settlementJob, jobParameters);
        return "Settlement Batch Job started.";
    }
}
