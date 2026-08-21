# CrowdShield — Current Status

## Accepted Phases
- **Phase 0** — Repository audit: COMPLETE
- **Phase 1** — Architecture skeleton / backend vertical slice: COMPLETE
- **Phase 2** — Scenario playback: COMPLETE
- **Phase 3** — Crowd Analytics: COMPLETE
- **Phase 4** — Risk Engine Refinement: COMPLETE

## Verified State
- Backend tests pass (49/49) (`.\gradlew.bat --no-daemon --console=plain test`)
- Backend builds (`.\gradlew.bat --no-daemon --console=plain bootJar`)
- Scenario playback works (4 scenarios: normal flow, entry bottleneck, counterflow panic, post-intervention recovery)
- Timeline risk scoring works (peak-aware, z-corridor-1 reaches CRITICAL in bottleneck scenario)
- Alerts return cleanly, deduped, with persistence verification (single anomalous bad frames do not trigger alerts)
- Hysteresis downgrade logic prevents rapid frame-to-frame flickering; sustained improvement required to downgrade
- Factor descriptions explain which dynamic factors drive risk and by how much
- Human-readable risk horizon estimates ("immediate", "1-2 min", "2-4 min", etc.)
- Safety disclaimer present on risk payloads
- Runtime configuration endpoints (`GET /api/risk/config` and `POST /api/risk/config`) validated and active
- SSE emits playback and risk events
- Crowd analytics live: density/inflow-outflow trends, net pressure, speed drop %, queue growth, movement instability, bottleneck/counterflow detection

## Key Files (Phase 4)
- `backend/src/main/java/com/crowdshield/config/CrowdShieldProperties.java`
- `backend/src/main/java/com/crowdshield/model/RiskConfigDto.java`
- `backend/src/main/java/com/crowdshield/model/OverallRisk.java`
- `backend/src/main/java/com/crowdshield/model/ZoneRisk.java`
- `backend/src/main/java/com/crowdshield/model/RiskResponse.java`
- `backend/src/main/java/com/crowdshield/service/RiskScoringService.java`
- `backend/src/main/java/com/crowdshield/service/RiskStateService.java`
- `backend/src/main/java/com/crowdshield/service/AlertService.java`
- `backend/src/main/java/com/crowdshield/web/ApiController.java`

## Endpoints (cumulative)
- `GET /api/risk/config` — current thresholds, weights, persistence, hysteresis settings
- `POST /api/risk/config` — update thresholds and weights at runtime
- `GET /api/risk/current` — risk score, level, trend, confidence, reasons, factor descriptions, horizon, disclaimer
- `GET /api/analytics/current` — analytics for all zones, detected bottlenecks/counterflow, overall summary
- `GET /api/analytics/zones/{zoneId}` — detailed zone analytics + historical timeline
- `GET /api/alerts` — active alerts with persistence and deduplication

## Repo
- Git initialized, pushed to https://github.com/sarthakksingh/CrowdShield.git

## Next Phase
**Phase 7 — Simulation & Intervention Loop** (or Phase 5 / 6 as prioritized)

## Explicitly Out of Scope For Now
Dashboard, Android app, YOLO/vision integration, database, auth, Kafka, Kubernetes.

## Hackathon Priority (2-day window)
Must-ship: Phase 7 → 8 → 9 → 13
Simplify if tight: Phase 5 (linear trend only), Phase 6 (fold into Phase 9 minimal), Phase 10, 11, 12