CREATE DATABASE IF NOT EXISTS shiguang_plan
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

USE shiguang_plan;

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
) COMMENT='用户表';

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
) COMMENT='目标表';

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
) COMMENT='打卡记录表';

INSERT INTO `user` (`username`, `password`, `nickname`, `role`) VALUES
('admin', 'e10adc3949ba59abbe56e057f20f883e', '管理员', 'admin'),
('zhangsan', 'e10adc3949ba59abbe56e057f20f883e', '张三', 'user'),
('lisi', 'e10adc3949ba59abbe56e057f20f883e', '李四', 'user'),
('wangwu', 'e10adc3949ba59abbe56e057f20f883e', '王五', 'user');

INSERT INTO `goal` (`user_id`, `title`, `description`, `start_date`, `end_date`, `target_days`, `status`) VALUES
(2, '每天背 50 个单词', '使用不背单词 App', '2026-07-01', '2026-08-31', 60, '进行中'),
(2, '刷 LeetCode 每日一题', '坚持每天一道算法题', '2026-07-10', '2026-09-10', 60, '进行中'),
(2, '阅读《代码整洁之道》', '每天读一章并做笔记', '2026-07-05', '2026-07-25', 20, '已完成'),
(3, '每天运动 30 分钟', '跑步或跳绳', '2026-07-01', '2026-08-01', 30, '进行中'),
(3, '学习 Spring Boot', '跟着教程完成一个小项目', '2026-07-01', '2026-08-15', 45, '进行中'),
(4, '每天写日记', '记录每日心得与反思', '2026-07-01', '2026-12-31', 180, '进行中');

INSERT INTO `checkin_record` (`user_id`, `goal_id`, `checkin_date`, `study_duration`, `content`) VALUES
(2, 1, '2026-07-10', 30, '背了 List 1-3'),
(2, 1, '2026-07-11', 25, '背了 List 4-6'),
(2, 1, '2026-07-12', 35, '背了 List 7-9'),
(2, 1, '2026-07-13', 20, '复习 List 1-9'),
(2, 1, '2026-07-14', 30, '背了 List 10-12'),
(2, 1, '2026-07-15', 40, '背了 List 13-15'),
(2, 1, '2026-07-16', 25, '复习本周全部单词');

INSERT INTO `checkin_record` (`user_id`, `goal_id`, `checkin_date`, `study_duration`, `content`) VALUES
(2, 2, '2026-07-10', 45, '两数之和、三数之和'),
(2, 2, '2026-07-11', 60, '最长回文子串'),
(2, 2, '2026-07-13', 30, '合并两个有序链表'),
(2, 2, '2026-07-15', 50, '二叉树的层序遍历'),
(2, 2, '2026-07-16', 40, '动态规划入门');

INSERT INTO `checkin_record` (`user_id`, `goal_id`, `checkin_date`, `study_duration`, `content`) VALUES
(3, 4, '2026-07-13', 30, '跑步 3 公里'),
(3, 4, '2026-07-14', 30, '跳绳 1000 个'),
(3, 4, '2026-07-15', 35, '跑步 4 公里'),
(3, 4, '2026-07-16', 30, '跳绳 1200 个');

INSERT INTO `checkin_record` (`user_id`, `goal_id`, `checkin_date`, `study_duration`, `remark`) VALUES
(4, 6, '2026-07-14', 15, '今天学了 Vue3 组件通信'),
(4, 6, '2026-07-15', 10, '整理了 Element Plus 常用组件'),
(4, 6, '2026-07-16', 20, '复习了 Vue Router 路由守卫');
