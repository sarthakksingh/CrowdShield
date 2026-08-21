@echo off
title CrowdShield Authority Dashboard
echo ====================================================
echo Starting CrowdShield Authority Dashboard (React/Vite)...
echo Dashboard URL: http://localhost:5173
echo ====================================================

cd /d "%~dp0dashboard"

if not exist "node_modules" (
    echo Installing dashboard dependencies...
    call npm install
)

echo Starting Vite dev server and launching browser...
call npm run dev -- --open
pause
