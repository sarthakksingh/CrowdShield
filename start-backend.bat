@echo off
title CrowdShield Backend Service
echo ====================================================
echo Starting CrowdShield Spring Boot Backend Service...
echo API Base: http://localhost:8080
echo SSE Stream: http://localhost:8080/api/stream
echo ====================================================

cd /d "%~dp0backend"
call gradlew.bat bootRun --console=plain
pause
