# CrowdShield — Current Status

## Accepted Phases
- **Phase 0** — Repository audit: COMPLETE
- **Phase 1** — Architecture skeleton / backend vertical slice: COMPLETE
- **Phase 2** — Scenario playback: COMPLETE
- **Phase 3** — Crowd Analytics: COMPLETE

## Verified State
- Backend tests pass (`.\gradlew.bat --no-daemon --console=plain test`)
- Backend builds (`.\gradlew.bat --no-daemon --console=plain bootJar`)
- Scenario playback works (4 scenarios: normal flow, entry bottleneck, counterflow panic, post-intervention recovery)
- Timeline risk scoring works (peak-aware, z-corridor-1 reaches CRITICAL in bottleneck scenario)
- Alerts return cleanly, deduped
- SSE emits playback and risk events
- Crowd analytics (`/api/analytics/current` and `/api/analytics/zones/{zoneId}`) computes density trends, inflow/outflow trends, net pressure, speed drop percentage, queue growth, bottleneck detection with explainability reasons, counterflow detection, movement instability, and zone timeline history

## Key Files (Phase 0-3)
- `backend/src/main/java/com/crowdshield/service/CrowdAnalyticsService.java`
- `backend/src/main/java/com/crowdshield/web/AnalyticsController.java`
- `backend/src/main/java/com/crowdshield/model/ZoneAnalytics.java`
- `backend/src/main/java/com/crowdshield/model/BottleneckInfo.java`
- `backend/src/main/java/com/crowdshield/model/CounterflowInfo.java`
- `backend/src/main/java/com/crowdshield/service/ScenarioPlaybackService.java`
- `backend/src/main/java/com/crowdshield/service/DemoDataService.java`
- `backend/src/main/java/com/crowdshield/service/RiskScoringService.java`
- `backend/src/main/java/com/crowdshield/service/RiskStateService.java`
- `backend/src/main/java/com/crowdshield/service/AlertService.java`
- `backend/src/main/java/com/crowdshield/web/PlaybackController.java`
- `backend/src/main/java/com/crowdshield/web/ApiExceptionHandler.java`
- `demo-data/scenarios/s1_normal_flow.json`, `s2_entry_bottleneck.json`, `s3_counterflow_panic.json`, `s4_post_intervention_recovery.json`

## Repo
- Git initialized, pushed to https://github.com/sarthakksingh/CrowdShield.git

## Next Phase
**Phase 4 — Predictive Analytics / Forecasting**

## Explicitly Out of Scope For Now
Dashboard, Android app, YOLO/vision integration, database, auth, Kafka, Kubernetes.

## Hackathon Priority (2-day window)
Must-ship: Phase 3 → 4 → 7 → 8 → 9 → 13
Simplify if tight: Phase 5 (linear trend only), Phase 6 (fold into Phase 9 minimal), Phase 10, 11, 12