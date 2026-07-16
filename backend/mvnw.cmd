@ECHO OFF
SETLOCAL

SET SCRIPT_DIR=%~dp0
SET POWERSHELL_EXE=powershell.exe

"%POWERSHELL_EXE%" -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%.mvn\wrapper\MavenWrapper.ps1" %*

EXIT /B %ERRORLEVEL%
