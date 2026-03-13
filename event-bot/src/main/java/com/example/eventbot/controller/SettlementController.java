package com.example.eventbot.controller;

import com.example.eventbot.dto.request.SettlementSettingsRequest;
import com.example.eventbot.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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

    @PostMapping("/update-settings")
    public String updateSettings(SettlementSettingsRequest request) {
        settlementService.updateSettings(request);
        return "redirect:/settlement";
    }

    @PostMapping("/start")
    public String start() {
        settlementService.startGeneration();
        return "redirect:/settlement";
    }

    @PostMapping("/stop")
    public String stop() {
        settlementService.stopGeneration();
        return "redirect:/settlement";
    }
}
