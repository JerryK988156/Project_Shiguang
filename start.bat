@echo off
chcp 65001 >nul
title 拾光计划 - 一键启动

echo ========================================
echo        拾光计划 - 一键启动脚本
echo ========================================
echo.
echo 正在启动后端服务 (Spring Boot :8080) ...
start "拾光计划-后端" cmd /k "cd /d %~dp0backend && call mvnw spring-boot:run"
echo.

echo 正在启动前端服务 (Vite :5173) ...
start "拾光计划-前端" cmd /k "cd /d %~dp0frontend && call npm run dev"
echo.

echo ========================================
echo  后端地址: http://localhost:8080
echo  前端地址: http://localhost:5173
echo  API 文档: http://localhost:8080/swagger-ui/index.html
echo ========================================
echo.
echo 两个服务已在独立窗口中启动。
echo 关闭此窗口不影响服务运行。
echo.
pause
