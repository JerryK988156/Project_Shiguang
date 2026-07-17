package com.example.shiguang.web.controller;

import com.example.shiguang.common.JsonResponse;
import com.example.shiguang.service.StatService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stat")
public class StatController {
    private final StatService statService;

    public StatController(StatService statService) {
        this.statService = statService;
    }

    @GetMapping("/overview")
    public JsonResponse<Map<String, Object>> overview() {
        return JsonResponse.success(statService.overview());
    }

    @GetMapping("/trend7")
    public JsonResponse<List<Map<String, Object>>> trend7() {
        return JsonResponse.success(statService.trend7());
    }

    @GetMapping("/goalProgress")
    public JsonResponse<List<Map<String, Object>>> goalProgress() {
        return JsonResponse.success(statService.goalProgress());
    }

    @GetMapping("/trend30")
    public JsonResponse<List<Map<String, Object>>> trend30() {
        return JsonResponse.success(statService.trend30());
    }

    @GetMapping("/goalTimeDistribution")
    public JsonResponse<List<Map<String, Object>>> goalTimeDistribution() {
        return JsonResponse.success(statService.goalTimeDistribution());
    }

    @GetMapping("/checkinCalendar")
    public JsonResponse<Map<String, Object>> checkinCalendar() {
        return JsonResponse.success(statService.checkinCalendar());
    }
}
