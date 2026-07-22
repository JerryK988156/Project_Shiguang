package com.example.shiguang.web.controller;

import com.example.shiguang.common.JsonResponse;
import com.example.shiguang.common.utls.SessionUtils;
import com.example.shiguang.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "后台管理", description = "管理员用户与系统概览接口")
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @Operation(summary = "系统概览")
    @GetMapping("/stat/overview")
    public JsonResponse<Map<String, Object>> overview() {
        SessionUtils.requireAdmin();
        return JsonResponse.success(adminService.overview());
    }

    @Operation(summary = "用户列表")
    @GetMapping("/user/list")
    public JsonResponse<List<Map<String, Object>>> userList() {
        SessionUtils.requireAdmin();
        return JsonResponse.success(adminService.userList());
    }

    @Operation(summary = "切换用户状态")
    @PostMapping("/user/status/{id}")
    public JsonResponse<Map<String, Object>> toggleUserStatus(@PathVariable Long id, @RequestParam("status") Integer status) {
        SessionUtils.requireAdmin();
        return JsonResponse.success(adminService.toggleUserStatus(id, status), "用户状态已更新");
    }
}
