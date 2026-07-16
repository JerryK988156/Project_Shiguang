package com.example.shiguang.web.controller;

import com.example.shiguang.common.JsonResponse;
import com.example.shiguang.service.AdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/stat/overview")
    public JsonResponse<Map<String, Object>> overview() {
        return JsonResponse.success(adminService.overview());
    }

    @GetMapping("/user/list")
    public JsonResponse<List<Map<String, Object>>> userList() {
        return JsonResponse.success(adminService.userList());
    }
}
