package com.example.shiguang.web.controller;

import com.example.shiguang.common.JsonResponse;
import com.example.shiguang.model.domain.Achievement;
import com.example.shiguang.service.AchievementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "成就系统", description = "成就徽章查询接口")
@RestController
@RequestMapping("/api/achievement")
public class AchievementController {
    private final AchievementService achievementService;

    public AchievementController(AchievementService achievementService) {
        this.achievementService = achievementService;
    }

    @Operation(summary = "获取成就列表")
    @GetMapping("/list")
    public JsonResponse<List<Achievement>> list() {
        return JsonResponse.success(achievementService.listByUser());
    }
}
