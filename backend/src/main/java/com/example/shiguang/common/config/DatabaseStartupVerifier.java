package com.example.shiguang.common.config;

import com.example.shiguang.common.service.DatabaseVerificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DatabaseStartupVerifier implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DatabaseStartupVerifier.class);

    private final DatabaseVerificationService databaseVerificationService;

    @Value("${app.database.verify-on-startup:true}")
    private boolean verifyOnStartup;

    @Value("${app.database.auto-init-on-startup:true}")
    private boolean autoInitOnStartup;

    public DatabaseStartupVerifier(DatabaseVerificationService databaseVerificationService) {
        this.databaseVerificationService = databaseVerificationService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!verifyOnStartup) {
            return;
        }

        Map<String, Object> result = databaseVerificationService.verify();
        log.info("阶段 1 数据库启动验证结果: {}", result);

        boolean connectionAvailable = Boolean.TRUE.equals(result.get("connectionAvailable"));
        boolean sampleDataAvailable = Boolean.TRUE.equals(result.get("sampleDataAvailable"));

        if (!autoInitOnStartup || (connectionAvailable && sampleDataAvailable)) {
            return;
        }

        Map<String, Object> initResult = databaseVerificationService.initialize(false);
        log.info("阶段 1 数据库自动初始化结果: {}", initResult);
    }
}
