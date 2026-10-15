@echo off
echo ========================================================
echo   Starting TaskFlow (High-Speed Mode)
echo ========================================================
cd /d "%~dp0backend"

if not exist "target\taskflow-0.0.1-SNAPSHOT.jar" (
    echo [INFO] JAR not found. Compiling package...
    call mvnw.cmd package -DskipTests
)

echo [INFO] Launching TaskFlow on http://localhost:8081 ...
java -XX:TieredStopAtLevel=1 -Dspring.profiles.active=dev -jar "target\taskflow-0.0.1-SNAPSHOT.jar"
pause
