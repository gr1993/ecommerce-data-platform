package com.example.eventbot.controller;

import com.example.eventbot.dto.request.SettlementSettingsRequest;
import com.example.eventbot.dto.response.SettlementSettingsResponse;
import com.example.eventbot.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/settlement")
@RequiredArgsConstructor
public class SettlementController {
    private final SettlementService settlementService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("settings", settlementService.getSettings());
        return "settlement";
    }

    @GetMapping("/status")
    @ResponseBody
    public SettlementSettingsResponse getStatus() {
        return settlementService.getSettingsResponse();
    }

    @PostMapping("/update-settings")
    public String updateSettings(SettlementSettingsRequest request) {
        settlementService.updateSettings(request);
        return "redirect:/settlement";
    }

    @PostMapping("/start")
    @ResponseBody
    public void start() {
        settlementService.startGeneration();
    }

    @PostMapping("/stop")
    @ResponseBody
    public void stop() {
        settlementService.stopGeneration();
    }
}
