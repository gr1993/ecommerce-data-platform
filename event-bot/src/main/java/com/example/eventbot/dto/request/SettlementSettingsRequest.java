package com.example.eventbot.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SettlementSettingsRequest {
    private int count;
    private int interval;
    private int perBatch;
    private double errorProb;
    private int cancelCount;
    private String startDate;
    private String endDate;
}
