package com.example.eventbot.controller;

import com.example.eventbot.dto.response.EventGeneratorResponse;
import com.example.eventbot.service.EventGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventGeneratorController {
    private final EventGeneratorService eventGeneratorService;

    @GetMapping
    public String index() {
        return "events";
    }

    @GetMapping("/status")
    @ResponseBody
    public EventGeneratorResponse getStatus() {
        return eventGeneratorService.getStatus();
    }

    @PostMapping("/start")
    @ResponseBody
    public void start() {
        eventGeneratorService.start();
    }

    @PostMapping("/stop")
    @ResponseBody
    public void stop() {
        eventGeneratorService.stop();
    }
}
