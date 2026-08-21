#!/usr/bin/env bash
echo "===================================================="
echo "Starting CrowdShield Spring Boot Backend Service..."
echo "API Base: http://localhost:8080"
echo "SSE Stream: http://localhost:8080/api/stream"
echo "===================================================="

cd "$(dirname "$0")/backend" || exit 1
chmod +x gradlew
./gradlew bootRun --console=plain
