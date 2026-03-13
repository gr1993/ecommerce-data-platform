package com.example.eventbot.controller;

import com.example.eventbot.service.SettlementService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/settlement")
public class SettlementController {
    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("settings", settlementService.getSettings());
        return "settlement";
    }

    @PostMapping("/update-settings")
    public String updateSettings(@RequestParam String topic, @RequestParam int count, @RequestParam double errorProb) {
        settlementService.updateSettings(topic, count, errorProb);
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
