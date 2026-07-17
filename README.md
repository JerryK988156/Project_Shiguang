# 拾光计划

> 学习打卡与目标管理系统 — 帮助学生设定学习目标、每日打卡、追踪学习进度。

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3 + Vite + Element Plus + Pinia + Vue Router + Axios + ECharts + Sass |
| 后端 | Spring Boot 3 + MyBatis-Plus |
| 数据库 | OceanBase（MySQL 兼容模式） |
| 鉴权 | Session 登录 |

## 目录结构

```
拾光计划/
├── frontend/          # 前端项目 (Vue 3)
│   └── src/
│       ├── api/       # 接口封装
│       ├── router/    # 路由配置
│       ├── stores/    # Pinia 状态管理
│       ├── utils/     # Axios 封装
│       └── views/     # 页面组件
├── backend/           # 后端项目 (Spring Boot)
│   └── src/main/
│       ├── java/.../  # Java 源码
│       └── resources/
│           ├── sql/   # 数据库初始化脚本
│           └── application.yml
└── docs/              # 项目文档
```

## 环境要求

- **Java** ≥ 21
- **Maven** ≥ 3.8
- **Node.js** ≥ 18
- **MySQL / OceanBase**（使用 MySQL 兼容连接）

## 数据库配置

1. 在 MySQL/OceanBase 中创建数据库（如果不存在会自动创建）：

```sql
CREATE DATABASE IF NOT EXISTS shiguang_plan
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;
```

2. 修改 `backend/src/main/resources/application.yml` 中的数据库连接信息，或通过环境变量覆盖：

| 环境变量 | 默认值 | 说明 |
|----------|--------|------|
| `OCEANBASE_URL` | `jdbc:mysql://127.0.0.1:2881/shiguang_plan?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai` | 数据库连接地址 |
| `OCEANBASE_USERNAME` | `root` | 数据库用户名 |
| `OCEANBASE_PASSWORD` | （空） | 数据库密码 |
| `DB_AUTO_INIT_ON_STARTUP` | `true` | 首次启动自动建表并插入测试数据 |

> 首次启动时，后端自动执行 `resources/sql/bootstrap.sql` 脚本创建表结构和测试数据。
> 设置 `DB_AUTO_INIT_ON_STARTUP=false` 可关闭自动初始化。

## 启动后端

```bash
cd backend

# Windows
mvnw spring-boot:run

# macOS / Linux
./mvnw spring-boot:run
```

后端启动后默认监听 `http://localhost:8080`。

## 启动前端

```bash
cd frontend

npm install        # 首次运行需安装依赖
npm run dev        # 启动开发服务器
```

前端开发服务器默认监听 `http://localhost:5173`，通过 Vite 代理将 `/api` 请求转发到后端 `http://localhost:8080`。

## 测试账号

通过启动时自动初始化的 `bootstrap.sql` 预置以下账号：

| 用户名 | 密码 | 角色 | 说明 |
|--------|------|------|------|
| `Jerry` | `794613` | 超级管理员 | 可管理所有用户（包括管理员） |
| `admin` | `123456` | 普通管理员 | 仅可管理普通用户 |
| `zhangsan` | `123456` | 普通用户 | 预置了目标和打卡数据 |
| `lisi` | `123456` | 普通用户 | 预置了目标和打卡数据 |
| `wangwu` | `123456` | 普通用户 | 预置了打卡数据 |

## 功能概览

- 用户登录 / 退出 / 修改资料 / 修改密码
- 目标增删改查与状态管理（进行中 / 已完成 / 已放弃）
- 每日打卡（同任务每天限打卡一次）
- 打卡记录查看与删除
- 个人统计概览与 ECharts 图表
- 管理员后台（用户列表 / 系统概览）

## 文档

- `docs/项目规划.md` — 完整项目规划与接口设计
- `docs/SQL设计稿.md` — 数据库表结构设计
- `docs/工程目录规划.md` — 前后端目录规划说明
