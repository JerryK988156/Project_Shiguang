package com.example.shiguang.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Phase 2 数据库迁移：创建 agent_message 和 audit_log 表
 */
@Component
public class Phase2Migration implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(Phase2Migration.class);
    private final JdbcTemplate jdbcTemplate;

    public Phase2Migration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS agent_message (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user_id BIGINT NOT NULL,
                    session_id VARCHAR(64) NOT NULL,
                    role VARCHAR(16) NOT NULL COMMENT '角色',
                    content TEXT NOT NULL COMMENT '消息内容',
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_user_session (user_id, session_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能助手聊天历史'
                """);
            log.info("Phase 2: agent_message 表已就绪");

            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS audit_log (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user_id BIGINT NOT NULL,
                    action VARCHAR(64) NOT NULL COMMENT '操作名称',
                    target VARCHAR(255) DEFAULT '' COMMENT '操作目标',
                    ip VARCHAR(64) DEFAULT '' COMMENT '客户端IP',
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_user_id (user_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志'
                """);
            log.info("Phase 2: audit_log 表已就绪");
        } catch (Exception e) {
            log.warn("Phase 2 迁移失败（可能表已存在）: {}", e.getMessage());
        }
    }
}
