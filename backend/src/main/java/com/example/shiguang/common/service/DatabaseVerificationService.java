package com.example.shiguang.common.service;

import com.example.shiguang.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DatabaseVerificationService {
    private static final Logger log = LoggerFactory.getLogger(DatabaseVerificationService.class);

    private final JdbcTemplate jdbcTemplate;
    private final UserMapper userMapper;

    @Value("${spring.datasource.url}")
    private String dataSourceUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${app.database.bootstrap-url}")
    private String bootstrapUrl;

    @Value("${app.database.target-database}")
    private String targetDatabase;

    public DatabaseVerificationService(JdbcTemplate jdbcTemplate, UserMapper userMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.userMapper = userMapper;
    }

    public Map<String, Object> verify() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("datasourceUrl", sanitizeJdbcUrl(dataSourceUrl));
        result.put("targetDatabase", targetDatabase);

        try {
            Integer selectOne = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            result.put("connectionAvailable", true);
            result.put("selectOne", selectOne);

            boolean userTableExists = hasUserTable();
            result.put("userTableExists", userTableExists);

            if (userTableExists) {
                Long userCount = userMapper.selectCount(null);
                result.put("mybatisQueryAvailable", true);
                result.put("userCount", userCount);
                result.put("sampleDataAvailable", userCount != null && userCount > 0);
            } else {
                result.put("mybatisQueryAvailable", false);
                result.put("sampleDataAvailable", false);
            }
        } catch (Exception ex) {
            result.put("connectionAvailable", false);
            result.put("mybatisQueryAvailable", false);
            result.put("sampleDataAvailable", false);
            result.put("error", rootMessage(ex));
            log.warn("数据库验证失败: {}", rootMessage(ex));
        }

        return result;
    }

    public Map<String, Object> initialize(boolean force) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("force", force);
        result.put("bootstrapUrl", sanitizeJdbcUrl(bootstrapUrl));
        result.put("targetDatabase", targetDatabase);

        try {
            createDatabaseIfNeeded();

            try (Connection connection = DriverManager.getConnection(dataSourceUrl, username, password)) {
                if (!force && hasUsableData(connection)) {
                    result.put("initialized", false);
                    result.put("message", "检测到 user 表和基础数据已存在，跳过初始化");
                } else {
                    connection.createStatement().execute("SET NAMES utf8mb4");
                    executeSqlScript(connection, "sql/bootstrap.sql");
                    result.put("initialized", true);
                    result.put("message", force ? "已强制重建数据库结构并导入测试数据" : "已初始化数据库结构并导入测试数据");
                }
            }

            result.put("verification", verify());
        } catch (Exception ex) {
            result.put("initialized", false);
            result.put("error", rootMessage(ex));
            log.warn("数据库初始化失败: {}", rootMessage(ex));
        }

        return result;
    }

    /**
     * 自定义 SQL 执行器，替代 ScriptUtils 以避免 charset 丢失问题。
     * 逐条读取 UTF-8 编码的 SQL 文件，以分号分隔后在同一个连接上顺序执行。
     */
    private void executeSqlScript(Connection connection, String classpath) throws Exception {
        ClassPathResource resource = new ClassPathResource(classpath);
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                    continue;
                }
                sb.append(line).append('\n');
            }
        }

        String[] statements = sb.toString().split(";");
        try (Statement stmt = connection.createStatement()) {
            for (String sql : statements) {
                String trimmed = sql.trim();
                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed);
                }
            }
        }
    }

    private void createDatabaseIfNeeded() throws Exception {
        validateDatabaseName(targetDatabase);
        String sql = "CREATE DATABASE IF NOT EXISTS `" + targetDatabase
                + "` DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci";

        try (Connection connection = DriverManager.getConnection(bootstrapUrl, username, password);
             Statement statement = connection.createStatement()) {
            statement.execute("SET NAMES utf8mb4");
            statement.execute(sql);
        }
    }

    private boolean hasUsableData(Connection connection) {
        Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'user'",
                Integer.class
        );
        if (tableCount == null || tableCount == 0) {
            return false;
        }

        Long userCount = userMapper.selectCount(null);
        return userCount != null && userCount > 0;
    }

    private boolean hasUserTable() {
        Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'user'",
                Integer.class
        );
        return tableCount != null && tableCount > 0;
    }

    private void validateDatabaseName(String databaseName) {
        if (databaseName == null || !databaseName.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("非法数据库名: " + databaseName);
        }
    }

    private String sanitizeJdbcUrl(String jdbcUrl) {
        return jdbcUrl.replaceAll("(?i)(password=)[^&]+", "$1***");
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
}
