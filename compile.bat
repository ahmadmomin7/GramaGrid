@echo off
setlocal enabledelayedexpansion

echo ===================================================
echo Compiling Rural Community Support Platform
echo ===================================================

cd /d "%~dp0"

if not exist "bin" mkdir bin
if not exist "bin\com\ruralhelp" mkdir bin\com\ruralhelp

echo Searching for Java source files...
dir /s /b src\main\java\*.java > sources.txt

echo Running javac compiler...
javac -encoding UTF-8 -cp "lib\mysql-connector-j-8.4.0.jar" -d bin @sources.txt

if %ERRORLEVEL% equ 0 (
    echo Compilation successful!
    del sources.txt
    if exist "src\main\resources\db.properties" (
        copy /y "src\main\resources\db.properties" "bin\db.properties" >nul
    )
    if exist "db.properties" (
        copy /y "db.properties" "bin\db.properties" >nul
    )
    echo Build output ready in bin/
) else (
    echo [ERROR] Compilation failed! Check error messages above.
    del sources.txt
    if not "%1"=="--nopause" pause
    exit /b 1
)

if not "%1"=="--nopause" pause
