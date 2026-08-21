# CrowdShield Backend (Phase 1)

Spring Boot backend for deterministic prototype flow:
- load venue + scenario JSON from `../demo-data`
- compute explainable risk score with factor contributions
- generate deduplicated alerts with hysteresis-aware levels
- expose REST endpoints and SSE stream

## Run

```bash
./gradlew bootRun
```

## Test

```bash
./gradlew test
```
