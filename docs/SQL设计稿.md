# 拾光计划 — OceanBase SQL 设计稿

> 数据库：OceanBase（MySQL 兼容模式）
> 字符集：utf8mb4
> 排序规则：utf8mb4_general_ci
> 引擎说明：OceanBase 不强制指定 ENGINE，下文 DDL 中省略 ENGINE 子句

---

## 1. 建库

```sql
CREATE DATABASE IF NOT EXISTS shiguang_plan
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

USE shiguang_plan;
```

---

## 2. 用户表 `user`

```sql
CREATE TABLE `user` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键',
  `username`    VARCHAR(50)  NOT NULL                 COMMENT '登录账号',
  `password`    VARCHAR(100) NOT NULL                 COMMENT '登录密码（加密存储）',
  `nickname`    VARCHAR(50)  DEFAULT NULL             COMMENT '昵称',
  `role`        VARCHAR(20)  NOT NULL DEFAULT 'user'  COMMENT '角色：admin / user',
  `avatar`      VARCHAR(255) DEFAULT NULL             COMMENT '头像地址',
  `profile`     VARCHAR(255) DEFAULT NULL             COMMENT '个性签名 / 简介',
  `status`      TINYINT      NOT NULL DEFAULT 1       COMMENT '状态：1 正常，0 禁用',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) COMMENT='用户表';
```

---

## 3. 目标表 `goal`

```sql
CREATE TABLE `goal` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键',
  `user_id`      BIGINT       NOT NULL                 COMMENT '所属用户 id',
  `title`        VARCHAR(100) NOT NULL                 COMMENT '目标标题',
  `description`  VARCHAR(500) DEFAULT NULL             COMMENT '目标说明',
  `start_date`   DATE         NOT NULL                 COMMENT '开始日期',
  `end_date`     DATE         DEFAULT NULL             COMMENT '结束日期（NULL 表示长期目标）',
  `target_days`  INT          DEFAULT NULL             COMMENT '计划坚持天数',
  `status`       VARCHAR(20)  NOT NULL DEFAULT '进行中' COMMENT '状态：进行中 / 已完成 / 已放弃',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) COMMENT='目标表';
```

---

## 4. 打卡记录表 `checkin_record`

```sql
CREATE TABLE `checkin_record` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键',
  `user_id`         BIGINT       NOT NULL                 COMMENT '用户 id',
  `goal_id`         BIGINT       NOT NULL                 COMMENT '关联目标 id',
  `checkin_date`    DATE         NOT NULL                 COMMENT '打卡日期',
  `study_duration`  INT          NOT NULL DEFAULT 0       COMMENT '学习时长（分钟）',
  `content`         VARCHAR(500) DEFAULT NULL             COMMENT '学习内容',
  `remark`          VARCHAR(500) DEFAULT NULL             COMMENT '备注 / 心得',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_goal_date` (`user_id`, `goal_id`, `checkin_date`),
  KEY `idx_goal_id` (`goal_id`),
  KEY `idx_checkin_date` (`checkin_date`)
) COMMENT='打卡记录表';
```

### 关键约束说明

- **唯一索引 `uk_user_goal_date`**：由 `(user_id, goal_id, checkin_date)` 组成。
  确保同一用户在同一日期对同一任务只能有一条打卡记录，从数据库层面杜绝重复打卡。
- 插入前业务层也应做一次存在性校验，避免依赖唯一索引抛异常来控制流程。

---

## 5. 索引汇总

| 表名 | 索引名 | 索引类型 | 字段 | 用途 |
| --- | --- | --- | --- | --- |
| user | PRIMARY | 主键 | id | 主键查询 |
| user | uk_username | 唯一 | username | 登录校验，防止重名 |
| goal | PRIMARY | 主键 | id | 主键查询 |
| goal | idx_user_id | 普通 | user_id | 按用户查目标列表 |
| goal | idx_status | 普通 | status | 按状态筛选 |
| checkin_record | PRIMARY | 主键 | id | 主键查询 |
| checkin_record | uk_user_goal_date | 唯一 | user_id, goal_id, checkin_date | 防重复打卡 |
| checkin_record | idx_goal_id | 普通 | goal_id | 按目标查打卡 |
| checkin_record | idx_checkin_date | 普通 | checkin_date | 按日期范围统计 |

---

## 6. 测试数据

### 6.1 预置用户

```sql
INSERT INTO `user` (`username`, `password`, `nickname`, `role`) VALUES
('admin',    'e10adc3949ba59abbe56e057f20f883e', '管理员',   'admin'),
('zhangsan', 'e10adc3949ba59abbe56e057f20f883e', '张三',     'user'),
('lisi',     'e10adc3949ba59abbe56e057f20f883e', '李四',     'user'),
('wangwu',   'e10adc3949ba59abbe56e057f20f883e', '王五',     'user');
```

> 密码统一为 `123456` 的 MD5（演示用，生产环境请改用 BCrypt）。

### 6.2 预置目标

```sql
INSERT INTO `goal` (`user_id`, `title`, `description`, `start_date`, `end_date`, `target_days`, `status`) VALUES
(2, '每天背 50 个单词',   '使用不背单词 App',       '2026-07-01', '2026-08-31', 60, '进行中'),
(2, '刷 LeetCode 每日一题', '坚持每天一道算法题',     '2026-07-10', '2026-09-10', 60, '进行中'),
(2, '阅读《代码整洁之道》',  '每天读一章并做笔记',     '2026-07-05', '2026-07-25', 20, '已完成'),
(3, '每天运动 30 分钟',    '跑步或跳绳',            '2026-07-01', '2026-08-01', 30, '进行中'),
(3, '学习 Spring Boot',   '跟着教程完成一个小项目',   '2026-07-01', '2026-08-15', 45, '进行中'),
(4, '每天写日记',          '记录每日心得与反思',      '2026-07-01', '2026-12-31', 180, '进行中');
```

### 6.3 预置打卡记录（近 7 天模拟数据）

```sql
-- 张三：背单词打卡 7 天连续
INSERT INTO `checkin_record` (`user_id`, `goal_id`, `checkin_date`, `study_duration`, `content`) VALUES
(2, 1, '2026-07-10', 30, '背了 List 1-3'),
(2, 1, '2026-07-11', 25, '背了 List 4-6'),
(2, 1, '2026-07-12', 35, '背了 List 7-9'),
(2, 1, '2026-07-13', 20, '复习 List 1-9'),
(2, 1, '2026-07-14', 30, '背了 List 10-12'),
(2, 1, '2026-07-15', 40, '背了 List 13-15'),
(2, 1, '2026-07-16', 25, '复习本周全部单词');

-- 张三：LeetCode 打卡 5 天
INSERT INTO `checkin_record` (`user_id`, `goal_id`, `checkin_date`, `study_duration`, `content`) VALUES
(2, 2, '2026-07-10', 45, '两数之和、三数之和'),
(2, 2, '2026-07-11', 60, '最长回文子串'),
(2, 2, '2026-07-13', 30, '合并两个有序链表'),
(2, 2, '2026-07-15', 50, '二叉树的层序遍历'),
(2, 2, '2026-07-16', 40, '动态规划入门');

-- 李四：运动打卡 4 天
INSERT INTO `checkin_record` (`user_id`, `goal_id`, `checkin_date`, `study_duration`, `content`) VALUES
(3, 4, '2026-07-13', 30, '跑步 3 公里'),
(3, 4, '2026-07-14', 30, '跳绳 1000 个'),
(3, 4, '2026-07-15', 35, '跑步 4 公里'),
(3, 4, '2026-07-16', 30, '跳绳 1200 个');

-- 王五：日记打卡 3 天
INSERT INTO `checkin_record` (`user_id`, `goal_id`, `checkin_date`, `study_duration`, `remark`) VALUES
(4, 6, '2026-07-14', 15, '今天学了 Vue3 组件通信'),
(4, 6, '2026-07-15', 10, '整理了 Element Plus 常用组件'),
(4, 6, '2026-07-16', 20, '复习了 Vue Router 路由守卫');
```

---

## 7. 常用统计查询示例

### 7.1 用户近 7 天每日打卡次数

```sql
SELECT checkin_date, COUNT(*) AS cnt
FROM checkin_record
WHERE user_id = 2 AND checkin_date >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
GROUP BY checkin_date
ORDER BY checkin_date;
```

### 7.2 用户累计学习时长

```sql
SELECT COALESCE(SUM(study_duration), 0) AS total_minutes
FROM checkin_record
WHERE user_id = 2;
```

### 7.3 用户连续打卡天数（最大连续）

```sql
-- 思路：按日期排序后计算日期差值分组，取最大连续组长度
-- 实现时建议在 Service 层用 Java 计算，或使用变量方式
```

### 7.4 各目标完成进度

```sql
SELECT g.id,
       g.title,
       g.target_days,
       COUNT(cr.id)                   AS checked_days,
       ROUND(COUNT(cr.id) / g.target_days * 100, 1) AS progress_pct
FROM goal g
LEFT JOIN checkin_record cr ON cr.goal_id = g.id
WHERE g.user_id = 2
GROUP BY g.id, g.title, g.target_days;
```

### 7.5 今日是否已对某任务打卡

```sql
SELECT COUNT(*) > 0 AS has_checked
FROM checkin_record
WHERE user_id = 2 AND goal_id = 1 AND checkin_date = CURDATE();
```

---

## 8. OceanBase 兼容注意事项

- OceanBase MySQL 模式支持标准的 `AUTO_INCREMENT`、`UNIQUE KEY`、`INDEX`，本文 DDL 可直接执行。
- `datetime` 类型精确到秒，已能满足本项目需求。
- `utf8mb4` 是 OceanBase 推荐的字符集，建议统一使用。
- 如果使用连接池（如 HikariCP），驱动仍用 `com.mysql.cj.jdbc.Driver`，配置方式与 MySQL 一致。
- OceanBase 不建议大量使用 `SELECT ... FOR UPDATE`，本项目无此场景，不影响。
