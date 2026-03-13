package com.example.eventbot.domain;

public class SettlementSettings {
    private String topic = "settlement-event";
    private int eventCount = 50;
    private double errorProbability = 0.1; // 10% 확률로 오류 데이터 생성
    private boolean running = false;

    // Getters and Setters
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public int getEventCount() { return eventCount; }
    public void setEventCount(int eventCount) { this.eventCount = eventCount; }
    public double getErrorProbability() { return errorProbability; }
    public void setErrorProbability(double errorProbability) { this.errorProbability = errorProbability; }
    public boolean isRunning() { return running; }
    public void setRunning(boolean running) { this.running = running; }
}
