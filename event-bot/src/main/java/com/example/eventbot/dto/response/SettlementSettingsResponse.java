package com.example.eventbot.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SettlementSettingsResponse {
    private int count;
    private int interval;
    private int perBatch;
    private double errorProb;
    private boolean running;

    private int processedCount;
    private int errorCount;
    private int totalTarget;
}
