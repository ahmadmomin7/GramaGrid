@echo off
cd /d "%~dp0"

echo ===================================================
echo Starting Rural Community Support Platform (CLI)
echo ===================================================

if not exist "bin\com\ruralhelp\Main.class" (
    echo Binaries not found. Running compilation first...
    call compile.bat
)

java -cp "bin;lib\mysql-connector-j-8.4.0.jar" com.ruralhelp.Main --cli
pause

