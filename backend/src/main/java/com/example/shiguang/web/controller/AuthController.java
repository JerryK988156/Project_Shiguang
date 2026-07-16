package com.example.shiguang.web.controller;

import com.example.shiguang.common.JsonResponse;
import com.example.shiguang.model.dto.LoginDTO;
import com.example.shiguang.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public JsonResponse<Map<String, Object>> login(@RequestBody LoginDTO loginDTO) {
        return JsonResponse.success(authService.login(loginDTO), "登录成功");
    }

    @PostMapping("/logout")
    public JsonResponse<Void> logout() {
        authService.logout();
        return JsonResponse.success(null, "退出成功");
    }

    @GetMapping("/userInfo")
    public JsonResponse<Map<String, Object>> userInfo() {
        return JsonResponse.success(authService.currentUser());
    }
}
