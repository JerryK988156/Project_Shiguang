package com.example.shiguang.web.controller;

import com.example.shiguang.common.BusinessException;
import com.example.shiguang.common.JsonResponse;
import com.example.shiguang.common.annotation.Auditable;
import com.example.shiguang.common.utls.JwtUtils;
import com.example.shiguang.model.dto.LoginDTO;
import com.example.shiguang.model.dto.RegisterDTO;
import com.example.shiguang.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "认证管理", description = "用户登录、注册、退出接口")
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Auditable(action = "登录")
    @PostMapping("/login")
    public JsonResponse<Map<String, Object>> login(@RequestBody LoginDTO loginDTO) {
        return JsonResponse.success(authService.login(loginDTO), "登录成功");
    }

    @PostMapping("/register")
    public JsonResponse<Map<String, Object>> register(@RequestBody RegisterDTO registerDTO) {
        return JsonResponse.success(authService.register(registerDTO), "注册成功");
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public JsonResponse<Void> logout() {
        authService.logout();
        return JsonResponse.success(null, "退出成功");
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/userInfo")
    public JsonResponse<Map<String, Object>> userInfo() {
        return JsonResponse.success(authService.currentUser());
    }

    @Operation(summary = "刷新令牌")
    @PostMapping("/refresh")
    public JsonResponse<Map<String, String>> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException("刷新令牌不能为空");
        }
        if (!JwtUtils.validate(refreshToken)) {
            throw new BusinessException("刷新令牌无效或已过期");
        }
        Long userId = JwtUtils.getUserIdFromExpiredToken(refreshToken);
        String role = JwtUtils.getUserRole(refreshToken);
        if (userId == null || role == null) {
            throw new BusinessException("刷新令牌无效");
        }
        String newAccessToken = JwtUtils.generateAccessToken(userId, role);
        String newRefreshToken = JwtUtils.generateRefreshToken(userId, role);
        return JsonResponse.success(
                Map.of("accessToken", newAccessToken, "refreshToken", newRefreshToken),
                "令牌刷新成功");
    }
}
