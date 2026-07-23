@echo off
title Shiguang Plan - Start

echo ========================================
echo    Shiguang Plan - One-Click Start
echo ========================================
echo.

set ROOT=%~dp0

echo Starting backend (Spring Boot :8080) ...
start "Backend" cmd /k "cd /d "%ROOT%backend" && .\mvnw spring-boot:run"
echo.

echo Starting frontend (Vite :5173) ...
start "Frontend" cmd /k "cd /d "%ROOT%frontend" && npm run dev"
echo.

echo ========================================
echo   Backend : http://localhost:8080
echo   Frontend: http://localhost:5173
echo   API Docs: http://localhost:8080/swagger-ui/index.html
echo ========================================
echo.
echo Both services started in separate windows.
echo You can close this window.
echo.
pause
