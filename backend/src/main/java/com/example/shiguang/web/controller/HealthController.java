package com.example.shiguang.web.controller;

import com.example.shiguang.common.JsonResponse;
import com.example.shiguang.common.service.DatabaseVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "健康检查", description = "系统与数据库健康检查接口")
@RestController
@RequestMapping("/api/health")
public class HealthController {
    private final DatabaseVerificationService databaseVerificationService;

    public HealthController(DatabaseVerificationService databaseVerificationService) {
        this.databaseVerificationService = databaseVerificationService;
    }

    @GetMapping
    public JsonResponse<Map<String, Object>> health() {
        Map<String, Object> data = new HashMap<>();
        data.put("project", "拾光计划");
        data.put("module", "backend");
        data.put("status", "UP");
        return JsonResponse.success(data);
    }

    @GetMapping("/db")
    public JsonResponse<Map<String, Object>> databaseHealth() {
        return JsonResponse.success(databaseVerificationService.verify());
    }

    @Operation(summary = "初始化数据库")
    @PostMapping("/db/init")
    public JsonResponse<Map<String, Object>> initializeDatabase(
            @RequestParam(defaultValue = "false") boolean force
    ) {
        return JsonResponse.success(databaseVerificationService.initialize(force));
    }
}
