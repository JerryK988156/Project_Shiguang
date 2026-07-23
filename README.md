# 拾光计划

> 学习打卡与目标管理系统 — 帮助学生设定学习目标、每日打卡、追踪学习进度。

[![版本](https://img.shields.io/badge/版本-v4.1-6366f1)](https://github.com/JerryK988156/Project_Shiguang)
[![测试](https://img.shields.io/badge/测试-12_PASS-brightgreen)](#测试)
[![构建](https://img.shields.io/badge/构建-passing-brightgreen)](#构建)

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3 + Vite 7 + Element Plus 2.11 + Pinia 3 + Vue Router 4 + Axios + ECharts 6 + Sass |
| 后端 | Spring Boot 3.4 + MyBatis-Plus 3.5 + SpringDoc OpenAPI 2.7 |
| 鉴权 | JWT（access token 2h + refresh token 7d + 黑名单登出撤销） |
| 密码加密 | BCrypt |
| 缓存 | Spring Cache + Redis（可选降级） |
| 数据库 | OceanBase（MySQL 兼容模式） |
| CI/CD | GitHub Actions |
| 部署 | Docker Compose（Redis + 后端 + 前端） |

## 目录结构

```
拾光计划/
├── README.md
├── start.bat              # Windows 一键启动脚本
├── .env.example           # 环境变量模板
├── docker-compose.yml     # Docker 部署配置
├── .github/workflows/     # CI/CD Pipeline
│
├── docs/                  # 项目文档（7 份）
│   ├── 项目规划.md
│   ├── SQL设计稿.md
│   ├── 工程目录规划.md
│   ├── 中期报告.md
│   ├── 进阶实现计划.md
│   ├── 优化方案.md
│   └── 最终报告.md
│
├── frontend/              # 前端项目 (Vue 3)
│   ├── src/
│   │   ├── api/           # 接口封装（7 个模块）
│   │   ├── components/    # 通用组件（AI 浮动聊天、骨架屏）
│   │   ├── router/        # 路由 + 守卫
│   │   ├── stores/        # Pinia 状态管理
│   │   ├── utils/         # Axios 封装 + 通知工具
│   │   └── views/         # 11 个页面组件
│   ├── vite.config.js
│   └── Dockerfile
│
└── backend/               # 后端项目 (Spring Boot)
    ├── src/main/
    │   ├── java/.../shiguang/
    │   │   ├── common/    # 统一返回 / 异常 / 鉴权 / 审计 / 缓存
    │   │   ├── model/     # 实体 + DTO
    │   │   ├── mapper/    # MyBatis-Plus 数据访问层
    │   │   ├── service/   # 业务逻辑层（8 个服务）
    │   │   └── web/       # 控制器层（9 个控制器，36 个 API）
    │   ├── resources/
    │   │   ├── sql/bootstrap.sql  # 数据库初始化（BCrypt 密码）
    │   │   └── application.yml
    │   └── test/          # 12 个单元/集成测试
    ├── pom.xml
    └── Dockerfile
```

## 环境要求

- **Java** ≥ 21
- **Maven** ≥ 3.8
- **Node.js** ≥ 18
- **MySQL / OceanBase**（使用 MySQL 兼容连接）
- **Redis**（可选，用于缓存和 Token 黑名单）

## 快速启动

### 方式一：一键启动（Windows）

双击项目根目录的 `start.bat`，自动在两个独立窗口中启动后端和前端。

### 方式二：手动启动

**1. 配置环境变量**

```bash
# 复制环境变量模板
cp .env.example .env
# 编辑 .env，填入真实的 AI_LLM_API_KEY（可选）
```

**2. 数据库**

在 MySQL/OceanBase 中创建数据库（首次启动会自动创建并初始化）：

```sql
CREATE DATABASE IF NOT EXISTS shiguang_plan
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;
```

**3. 启动后端**

```bash
cd backend

# Windows
mvnw spring-boot:run

# macOS / Linux
./mvnw spring-boot:run
```

后端默认监听 `http://localhost:8080`。

**4. 启动前端**

```bash
cd frontend

npm install        # 首次运行
npm run dev
```

前端默认监听 `http://localhost:5173`。

### 方式三：Docker 部署

```bash
cp .env.example .env     # 编辑填入 API Key
docker compose -p shiguang up -d
```

## 测试账号

| 用户名 | 密码 | 角色 | 说明 |
|--------|------|------|------|
| `Jerry` | `794613` | 超级管理员 | 可管理所有用户（含管理员） |
| `admin` | `123456` | 普通管理员 | 仅可管理普通用户 |
| `zhangsan` | `123456` | 普通用户 | 预置 3 个目标 + 19 条打卡 |
| `lisi` | `123456` | 普通用户 | 预置 2 个目标 + 11 条打卡 |
| `wangwu` | `123456` | 普通用户 | 预置 1 个目标 + 6 条打卡 |

> 密码已在 bootstrap.sql 中使用 BCrypt 加密存储。

## 功能概览

### 用户模块
- 登录 / 注册 / 退出（Token 黑名单撤销）
- 修改资料（昵称 / 头像 / 简介）
- 修改密码（BCrypt 加密）
- 三级权限（超级管理员 > 管理员 > 普通用户）

### 目标管理
- 新增 / 编辑 / 删除 / 详情 / 列表（含抽屉快速编辑）
- 状态切换（进行中 → 已完成 / 已放弃）
- 起止日期联动计算计划天数
- 标签分类（多标签支持，按标签筛选和统计）
- 目标模板库（10 个预设模板一键填充）
- 关键词搜索（标题 + 标签模糊匹配）

### 打卡系统
- 每日打卡（锁定系统当天日期，不可修改）
- 日期范围校验（仅限目标起止时间内可打卡）
- 打卡记录查看与删除
- CSV 数据导出
- 数据库 + 业务层双重唯一约束

### 统计分析（9 种图表）
- 数字概览卡片（目标数 / 打卡数 / 总时长 / 连续天数）
- 近 7 天 / 30 天打卡趋势（折线图 + 柱状图）
- 目标状态分布（饼图）
- 目标完成进度（柱状图）
- 各目标学习时长占比（环形饼图）
- 打卡日历热力图（GitHub 风格）
- 标签统计
- 学习周报（本周 vs 上周对比）

### 成就系统
- 6 级成就：青铜 🥉(7天) / 白银 🥈(14天) / 黄金 🥇(21天) / 钻石 💎(30天) / 大师 🏆(60天) / 传奇 👑(100天)
- 打卡触发自动检测 + canvas-confetti 庆祝动画

### AI 智能助手
- 基于 LLM Function Calling，支持 8 种工具函数
- 真正的流式输出（SSE stream: true）
- 浮动聊天窗口（可拖拽缩放）
- 对话历史持久化

### 工程化
- OpenAPI 3.0 文档（Swagger UI）
- AOP 审计日志
- Docker 容器化部署
- GitHub Actions CI（自动构建 + 测试）
- 移动端响应式布局
- 骨架屏加载 + 空状态引导
- 浏览器打卡提醒通知

## API 文档

启动后端后访问：
- Swagger UI：http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON：http://localhost:8080/v3/api-docs

## 测试

```bash
# 后端单元测试
cd backend
./mvnw test

# 前端构建验证
cd frontend
npm run build
```

当前测试覆盖：**12 个单元/集成测试全部通过**，36 个 API 端点全部手工验证通过。

## 环境变量参考

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `OCEANBASE_URL` | `jdbc:mysql://127.0.0.1:2881/shiguang_plan?...` | 数据库连接 |
| `OCEANBASE_USERNAME` | `root` | 数据库用户名 |
| `OCEANBASE_PASSWORD` | （空） | 数据库密码 |
| `REDIS_HOST` | `localhost` | Redis 地址 |
| `REDIS_PORT` | `6379` | Redis 端口 |
| `CACHE_TYPE` | `redis` | 缓存类型（redis/simple） |
| `AI_LLM_API_KEY` | （需配置） | DeepSeek API Key |
| `DB_AUTO_INIT_ON_STARTUP` | `true` | 首次启动自动建表 |

## 文档

- `docs/项目规划.md` — 完整项目规划与接口设计
- `docs/SQL设计稿.md` — 数据库表结构与索引
- `docs/工程目录规划.md` — 前后端目录规划
- `docs/中期报告.md` — 中期开发进度
- `docs/进阶实现计划.md` — 三阶段进阶计划
- `docs/优化方案.md` — v3.0→v4.0 优化方案
- `docs/最终报告.md` — 最终项目报告（v4.0）
