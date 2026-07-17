package com.example.shiguang.common.utls;

import com.example.shiguang.common.BusinessException;
import com.example.shiguang.model.domain.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class SessionUtils {
    public static final String LOGIN_USER_ID = "loginUserId";
    public static final String LOGIN_USER_ROLE = "loginUserRole";
    public static final String LOGIN_USERNAME = "loginUsername";
    public static final Long SUPER_ADMIN_ID = 1L;

    private SessionUtils() {
    }

    public static HttpSession session() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attributes.getRequest().getSession(true);
    }

    public static void login(User user) {
        HttpSession session = session();
        session.setAttribute(LOGIN_USER_ID, user.getId());
        session.setAttribute(LOGIN_USER_ROLE, user.getRole());
        session.setAttribute(LOGIN_USERNAME, user.getUsername());
    }

    public static void logout() {
        session().invalidate();
    }

    public static Long currentUserId() {
        Object value = session().getAttribute(LOGIN_USER_ID);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    public static Long requireUserId() {
        Long userId = currentUserId();
        if (userId == null) {
            throw new BusinessException("请先登录");
        }
        return userId;
    }

    public static String currentUserRole() {
        Object value = session().getAttribute(LOGIN_USER_ROLE);
        return value == null ? null : value.toString();
    }

    public static void requireAdmin() {
        if (!"admin".equals(currentUserRole())) {
            throw new BusinessException("无权限访问");
        }
    }

    /**
     * 超级管理员：id 为 1 且角色为 admin 的用户
     */
    public static boolean isSuperAdmin() {
        Long userId = currentUserId();
        return userId != null && SUPER_ADMIN_ID.equals(userId) && "admin".equals(currentUserRole());
    }
}
