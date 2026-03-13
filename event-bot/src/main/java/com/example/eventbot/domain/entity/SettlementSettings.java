package com.example.eventbot.domain.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SettlementSettings {
    private int eventCount = 10;           // 발행 횟수
    private int intervalSeconds = 1;       // 발행 간격(초)
    private int eventsPerBatch = 1;        // 1회 발행 이벤트 수
    private double errorProbability = 0.1; // 오류 데이터 발생 확률
    private boolean running = false;
}
