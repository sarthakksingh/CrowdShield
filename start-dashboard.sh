#!/usr/bin/env bash
echo "===================================================="
echo "Starting CrowdShield Authority Dashboard (React/Vite)..."
echo "Dashboard URL: http://localhost:5173"
echo "===================================================="

cd "$(dirname "$0")/dashboard" || exit 1

if [ ! -d "node_modules" ]; then
    echo "Installing dashboard dependencies..."
    npm install
fi

echo "Starting Vite dev server and launching browser..."
npm run dev -- --open
