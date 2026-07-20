SET NAMES utf8mb4;

DROP TABLE IF EXISTS `achievement`;
DROP TABLE IF EXISTS `checkin_record`;
DROP TABLE IF EXISTS `goal_tag`;
DROP TABLE IF EXISTS `goal`;
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` VARCHAR(50) NOT NULL COMMENT '登录账号',
  `password` VARCHAR(100) NOT NULL COMMENT '登录密码（加密存储）',
  `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
  `role` VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '角色：admin / user',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像地址',
  `profile` VARCHAR(255) DEFAULT NULL COMMENT '个性签名 / 简介',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 正常，0 禁用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE `goal` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '所属用户 id',
  `title` VARCHAR(100) NOT NULL COMMENT '目标标题',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '目标说明',
  `start_date` DATE NOT NULL COMMENT '开始日期',
  `end_date` DATE DEFAULT NULL COMMENT '结束日期（NULL 表示长期目标）',
  `target_days` INT DEFAULT NULL COMMENT '计划坚持天数',
  `status` VARCHAR(20) NOT NULL DEFAULT '进行中' COMMENT '状态：进行中 / 已完成 / 已放弃',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='目标表';

CREATE TABLE `goal_tag` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `goal_id` BIGINT NOT NULL COMMENT '关联目标 id',
  `tag_name` VARCHAR(30) NOT NULL COMMENT '标签名称',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_goal_tag` (`goal_id`, `tag_name`),
  KEY `idx_tag` (`tag_name`)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='目标标签表';

CREATE TABLE `checkin_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户 id',
  `goal_id` BIGINT NOT NULL COMMENT '关联目标 id',
  `checkin_date` DATE NOT NULL COMMENT '打卡日期',
  `study_duration` INT NOT NULL DEFAULT 0 COMMENT '学习时长（分钟）',
  `content` VARCHAR(500) DEFAULT NULL COMMENT '学习内容',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注 / 心得',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_goal_date` (`user_id`, `goal_id`, `checkin_date`),
  KEY `idx_goal_id` (`goal_id`),
  KEY `idx_checkin_date` (`checkin_date`)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='打卡记录表';

DROP TABLE IF EXISTS `achievement`;
CREATE TABLE `achievement` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `goal_id` BIGINT NOT NULL,
  `goal_title` VARCHAR(100) NOT NULL,
  `milestone_days` INT NOT NULL COMMENT '里程碑天数：7/14/21/30/60/100',
  `badge_name` VARCHAR(50) NOT NULL COMMENT '青铜/白银/黄金/钻石/大师/传奇',
  `achieved_date` DATE NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_goal_milestone` (`user_id`, `goal_id`, `milestone_days`),
  KEY `idx_user_id` (`user_id`)
) DEFAULT CHARSET=utf8mb4;

INSERT INTO `user` (`username`, `password`, `nickname`, `role`) VALUES
('Jerry', '953cdbd5747772fd609cfebf0a4384f9', 'Jerry', 'admin'),
('admin', 'e10adc3949ba59abbe56e057f20f883e', '管理员', 'admin'),
('zhangsan', 'e10adc3949ba59abbe56e057f20f883e', '张三', 'user'),
('lisi', 'e10adc3949ba59abbe56e057f20f883e', '李四', 'user'),
('wangwu', 'e10adc3949ba59abbe56e057f20f883e', '王五', 'user');

INSERT INTO `goal` (`user_id`, `title`, `description`, `start_date`, `end_date`, `target_days`, `status`) VALUES
(3, '每天背 50 个单词', '使用不背单词 App', '2026-07-01', '2026-08-31', 60, '进行中'),
(3, '刷 LeetCode 每日一题', '坚持每天一道算法题', '2026-07-10', '2026-09-10', 60, '进行中'),
(3, '阅读《代码整洁之道》', '每天读一章并做笔记', '2026-07-05', '2026-07-25', 20, '已完成'),
(4, '每天运动 30 分钟', '跑步或跳绳', '2026-07-01', '2026-08-01', 30, '进行中'),
(4, '学习 Spring Boot', '跟着教程完成一个小项目', '2026-07-01', '2026-08-15', 45, '进行中'),
(5, '每天写日记', '记录每日心得与反思', '2026-07-01', '2026-12-31', 180, '进行中');

-- zhangsan 每天背单词 (goal 1)，覆盖 7.1 至今
INSERT INTO `checkin_record` (`user_id`, `goal_id`, `checkin_date`, `study_duration`, `content`) VALUES
(3, 1, '2026-07-01', 20, '背了 List 1-2'),
(3, 1, '2026-07-02', 25, '背了 List 3-4'),
(3, 1, '2026-07-03', 30, '背了 List 5-7'),
(3, 1, '2026-07-04', 20, '复习 List 1-7'),
(3, 1, '2026-07-05', 35, '背了 List 8-10'),
(3, 1, '2026-07-06', 25, '背了 List 11-12'),
(3, 1, '2026-07-07', 15, '复习本周全部单词'),
(3, 1, '2026-07-08', 30, '背了 List 13-15'),
(3, 1, '2026-07-09', 25, '背了 List 16-18'),
(3, 1, '2026-07-10', 30, '背了 List 1-3（第二轮)'),
(3, 1, '2026-07-11', 25, '背了 List 4-6（第二轮)'),
(3, 1, '2026-07-12', 35, '背了 List 7-9（第二轮)'),
(3, 1, '2026-07-13', 20, '复习 List 1-9'),
(3, 1, '2026-07-14', 30, '背了 List 10-12（第二轮)'),
(3, 1, '2026-07-15', 40, '背了 List 13-15（第二轮)'),
(3, 1, '2026-07-16', 25, '复习本周全部单词'),
(3, 1, '2026-07-17', 30, '背了 List 16-18（第二轮)');

-- zhangsan 刷 LeetCode (goal 2)，隔天刷
INSERT INTO `checkin_record` (`user_id`, `goal_id`, `checkin_date`, `study_duration`, `content`) VALUES
(3, 2, '2026-07-01', 50, '两数之和、三数之和'),
(3, 2, '2026-07-03', 45, '最长回文子串'),
(3, 2, '2026-07-05', 60, '合并两个有序链表'),
(3, 2, '2026-07-07', 40, '有效的括号'),
(3, 2, '2026-07-09', 55, '爬楼梯、斐波那契'),
(3, 2, '2026-07-11', 60, '最长回文子串（复习)'),
(3, 2, '2026-07-13', 30, '合并两个有序链表（复习)'),
(3, 2, '2026-07-15', 50, '二叉树的层序遍历'),
(3, 2, '2026-07-17', 40, '动态规划入门');

-- zhangsan 阅读 (goal 3)，偶尔读
INSERT INTO `checkin_record` (`user_id`, `goal_id`, `checkin_date`, `study_duration`, `content`) VALUES
(3, 3, '2026-07-05', 45, '第1章：整洁代码'),
(3, 3, '2026-07-06', 30, '第2章：有意义的命名'),
(3, 3, '2026-07-07', 40, '第3章：函数'),
(3, 3, '2026-07-10', 35, '第4章：注释'),
(3, 3, '2026-07-12', 50, '第5章：格式'),
(3, 3, '2026-07-14', 45, '第6章：对象和数据结构'),
(3, 3, '2026-07-16', 40, '第7章：错误处理');

-- lisi 每天运动 (goal 4)
INSERT INTO `checkin_record` (`user_id`, `goal_id`, `checkin_date`, `study_duration`, `content`) VALUES
(4, 4, '2026-07-01', 30, '晨跑 3 公里'),
(4, 4, '2026-07-02', 30, '跳绳 1000 个'),
(4, 4, '2026-07-03', 35, '跑步 4 公里'),
(4, 4, '2026-07-04', 0, '休息日'),
(4, 4, '2026-07-05', 30, '跳绳 1200 个'),
(4, 4, '2026-07-07', 35, '跑步 4 公里'),
(4, 4, '2026-07-09', 30, '跳绳 1000 个'),
(4, 4, '2026-07-11', 40, '跑步 5 公里'),
(4, 4, '2026-07-13', 30, '跳绳 1500 个'),
(4, 4, '2026-07-15', 35, '跑步 4 公里'),
(4, 4, '2026-07-17', 30, '跳绳 1200 个');

-- lisi 学 Spring Boot (goal 5)
INSERT INTO `checkin_record` (`user_id`, `goal_id`, `checkin_date`, `study_duration`, `content`) VALUES
(4, 5, '2026-07-02', 45, 'Spring Boot 入门'),
(4, 5, '2026-07-05', 60, '自动配置原理'),
(4, 5, '2026-07-08', 50, 'Web 开发基础'),
(4, 5, '2026-07-12', 55, '数据访问层 MyBatis'),
(4, 5, '2026-07-15', 40, 'RESTful API 设计');

-- wangwu 每天写日记 (goal 6)，7.11-7.16 共 6 天，今天打卡第 7 次触发青铜成就
INSERT INTO `checkin_record` (`user_id`, `goal_id`, `checkin_date`, `study_duration`, `remark`) VALUES
(5, 6, '2026-07-11', 15, '学习了 Axios 封装'),
(5, 6, '2026-07-12', 10, '搭建了一个小项目'),
(5, 6, '2026-07-13', 20, '修复了几个 bug'),
(5, 6, '2026-07-14', 10, '优化了页面样式'),
(5, 6, '2026-07-15', 15, '写了单元测试'),
(5, 6, '2026-07-16', 12, '复习了 Vue Router 路由守卫');
