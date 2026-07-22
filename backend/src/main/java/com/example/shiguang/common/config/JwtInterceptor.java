package com.example.shiguang.common.config;

import com.example.shiguang.common.service.TokenBlacklistService;
import com.example.shiguang.common.utls.JwtUtils;
import com.example.shiguang.common.utls.SessionUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired(required = false)
    private TokenBlacklistService tokenBlacklistService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authHeader = request.getHeader("Authorization");
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            writeUnauthorized(response);
            return false;
        }

        String token = authHeader.substring(7);
        if (!JwtUtils.validate(token)) {
            writeUnauthorized(response);
            return false;
        }

        // 检查黑名单
        if (tokenBlacklistService != null && tokenBlacklistService.isBlacklisted(token)) {
            writeTokenInvalidated(response);
            return false;
        }

        Long userId = JwtUtils.getUserId(token);
        String role = JwtUtils.getUserRole(token);
        if (userId == null) {
            writeUnauthorized(response);
            return false;
        }

        request.setAttribute(SessionUtils.LOGIN_USER_ID, userId);
        request.setAttribute(SessionUtils.LOGIN_USER_ROLE, role);
        return true;
    }

    private void writeUnauthorized(HttpServletResponse response) throws Exception {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                Map.of("code", 401, "message", "请先登录", "status", false)
        ));
    }

    private void writeTokenInvalidated(HttpServletResponse response) throws Exception {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                Map.of("code", 401, "message", "Token 已失效", "status", false)
        ));
    }
}
