package com.example.eventbot.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class EventGeneratorResponse {
    private boolean running;
    private Map<String, Long> counts;
}
