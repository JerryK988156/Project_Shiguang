package com.example.shiguang.common.aspect;

import com.example.shiguang.common.annotation.Auditable;
import com.example.shiguang.common.utls.SessionUtils;
import com.example.shiguang.mapper.AuditLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Aspect
@Component
public class AuditAspect {

    private final AuditLogMapper auditLogMapper;

    public AuditAspect(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    @Around("@annotation(auditable)")
    public Object around(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        Object result = joinPoint.proceed();

        try {
            Long userId = SessionUtils.currentUserId();
            if (userId == null) {
                return result;
            }

            String target = auditable.target();

            // 解析占位符 #id 等简单变量
            if (target != null && target.startsWith("#")) {
                String varName = target.substring(1);
                String[] paramNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
                Object[] args = joinPoint.getArgs();
                if (paramNames != null) {
                    for (int i = 0; i < paramNames.length; i++) {
                        if (varName.equals(paramNames[i]) && args[i] != null) {
                            target = args[i].toString();
                            break;
                        }
                    }
                }
            }

            String ip = "";
            try {
                ServletRequestAttributes attributes =
                        (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
                HttpServletRequest request = attributes.getRequest();
                String xff = request.getHeader("X-Forwarded-For");
                ip = (xff != null && !xff.isBlank()) ? xff.split(",")[0].trim()
                        : request.getRemoteAddr();
            } catch (Exception ignored) {
            }

            com.example.shiguang.model.domain.AuditLog log = new com.example.shiguang.model.domain.AuditLog();
            log.setUserId(userId);
            log.setAction(auditable.action());
            log.setTarget(target);
            log.setIp(ip);
            log.setCreateTime(LocalDateTime.now());
            auditLogMapper.insert(log);
        } catch (Exception ignored) {
        }

        return result;
    }
}
