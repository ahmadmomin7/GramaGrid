@echo off
setlocal

echo ===================================================
echo Setting Up MySQL Database: rural_support_db
echo ===================================================

cd /d "%~dp0"

set MYSQL_CMD=mysql
where mysql >nul 2>nul
if %ERRORLEVEL% neq 0 (
    if exist "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" (
        set MYSQL_CMD="C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
    ) else if exist "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" (
        set MYSQL_CMD="C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
    ) else (
        echo [ERROR] MySQL command-line tool not found in PATH or standard Program Files!
        echo Please ensure MySQL Server is installed and mysql.exe is in your PATH.
        pause
        exit /b 1
    )
)

echo.
echo Executing sql\schema.sql on MySQL...
echo You will be prompted to enter your MySQL root password below:
echo.

%MYSQL_CMD% -u root -p < sql\schema.sql

if %ERRORLEVEL% equ 0 (
    echo.
    echo ===================================================
    echo Database 'rural_support_db' initialized successfully!
    echo Sample users, NGOs, and issues have been seeded.
    echo ===================================================
) else (
    echo.
    echo [ERROR] Failed to execute schema script. Please verify your password.
)

pause

