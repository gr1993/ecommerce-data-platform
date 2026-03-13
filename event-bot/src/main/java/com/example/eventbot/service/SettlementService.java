package com.example.eventbot.service;

import com.example.eventbot.domain.SettlementSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class SettlementService {
    private static final Logger log = LoggerFactory.getLogger(SettlementService.class);
    private final SettlementSettings settings = new SettlementSettings();
    private final Random random = new Random();

    public SettlementSettings getSettings() {
        return settings;
    }

    public void startGeneration() {
        if (settings.isRunning()) return;
        
        settings.setRunning(true);
        log.info("[정산] 이벤트 생성 시작: 토픽={}, 횟수={}, 오류확률={}", 
            settings.getTopic(), settings.getEventCount(), settings.getErrorProbability());
            
        new Thread(() -> {
            try {
                for (int i = 0; i < settings.getEventCount(); i++) {
                    if (!settings.isRunning()) break;
                    
                    boolean isError = random.nextDouble() < settings.getErrorProbability();
                    log.info("[정산] 이벤트 생성 {}/{} (오류여부: {})", i + 1, settings.getEventCount(), isError);
                    
                    Thread.sleep(200);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                settings.setRunning(false);
                log.info("[정산] 이벤트 생성 완료.");
            }
        }).start();
    }

    public void stopGeneration() {
        settings.setRunning(false);
        log.info("[정산] 이벤트 생성 사용자 중단.");
    }
    
    public void updateSettings(String topic, int count, double errorProb) {
        settings.setTopic(topic);
        settings.setEventCount(count);
        settings.setErrorProbability(errorProb);
        log.info("[정산] 설정 변경: 토픽={}, 횟수={}, 오류확률={}", topic, count, errorProb);
    }
}
