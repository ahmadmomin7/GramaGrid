@echo off
setlocal
cd /d "%~dp0"
echo Stopping any previous server on port 8080...
for /f "tokens=5" %%P in ('netstat -ano ^| findstr ":8080" ^| findstr "LISTENING"') do (
  echo Stopping process %%P
  taskkill /PID %%P /F >nul 2>&1
)
timeout /t 2 /nobreak >nul
echo Rebuilding website from current source...
call compile.bat --nopause
if errorlevel 1 (
  echo Compilation failed. Fix the errors above before continuing.
  pause
  exit /b 1
)
echo Starting website at http://localhost:8080/
start "" "http://localhost:8080/"
java -cp "bin;lib\mysql-connector-j-8.4.0.jar" com.ruralhelp.web.WebServer
pause
