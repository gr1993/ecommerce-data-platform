package com.example.eventbot.domain.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class SettlementSettings {
    private int eventCount = 10;           // 발행 횟수
    private int intervalSeconds = 1;       // 발행 간격(초)
    private int eventsPerBatch = 1;        // 1회 발행 이벤트 수
    private double errorProbability = 0.1; // 오류 데이터 발생 확률
    private int cancelCountTarget = 0;     // 취소 목표 건수 (추가)
    private LocalDate startDate = LocalDate.now(); // 시작일
    private LocalDate endDate = LocalDate.now().plusDays(7); // 종료일
    private boolean running = false;

    // 실시간 상태 추적용 (Stateless 원칙에 따라 메모리 유지)
    private int processedCount = 0;        // 현재까지 발행된 총 이벤트 수
    private int errorCount = 0;            // 현재까지 발생한 총 오류 수
    private int processedCancelCount = 0;  // 현재까지 발생한 총 취소 수 (추가)

    public int getTotalTargetCount() {
        return eventCount * eventsPerBatch;
    }

    public void resetCounts() {
        this.processedCount = 0;
        this.errorCount = 0;
        this.processedCancelCount = 0;
    }
}
