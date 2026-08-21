# CrowdShield — Current Status

## Accepted Phases
- **Phase 0** — Repository audit: COMPLETE
- **Phase 1** — Architecture skeleton / backend vertical slice: COMPLETE
- **Phase 2** — Scenario playback: COMPLETE
- **Phase 3** — Crowd Analytics: COMPLETE
- **Phase 4** — Risk Engine Refinement: COMPLETE
- **Phase 7** — Intervention Engine: COMPLETE

## Verified State
- Backend tests pass (58/58) (`.\gradlew.bat --no-daemon --console=plain test`)
- Backend builds (`.\gradlew.bat --no-daemon --console=plain bootJar`)
- Scenario playback works (4 scenarios: normal flow, entry bottleneck, counterflow panic, post-intervention recovery)
- Timeline risk scoring works (peak-aware, z-corridor-1 reaches CRITICAL in bottleneck scenario)
- Alerts return cleanly, deduped, with persistence verification
- Hysteresis downgrade logic prevents rapid frame-to-frame flickering
- Factor descriptions and risk horizon estimates populated
- Runtime risk configuration active (`GET /api/risk/config`, `POST /api/risk/config`)
- Crowd analytics active (`/api/analytics/current`, `/api/analytics/zones/{zoneId}`)
- Intervention engine active (`GET /api/recommendations/current`): Gate advisor, Route advisor, Personnel advisor, Announcement advisor, with prioritized impact ranking and strictly advisory language

## Key Files (Phase 7)
- `backend/src/main/java/com/crowdshield/model/RecommendationActionType.java`
- `backend/src/main/java/com/crowdshield/model/Recommendation.java`
- `backend/src/main/java/com/crowdshield/model/RecommendationsResponse.java`
- `backend/src/main/java/com/crowdshield/service/intervention/GateAdvisor.java`
- `backend/src/main/java/com/crowdshield/service/intervention/RouteAdvisor.java`
- `backend/src/main/java/com/crowdshield/service/intervention/PersonnelAdvisor.java`
- `backend/src/main/java/com/crowdshield/service/intervention/AnnouncementAdvisor.java`
- `backend/src/main/java/com/crowdshield/service/InterventionEngineService.java`
- `backend/src/main/java/com/crowdshield/web/RecommendationController.java`

## Endpoints (cumulative)
- `GET /api/recommendations/current` — prioritized, explainable crowd management advisories
- `GET /api/risk/config` — current thresholds, weights, persistence, hysteresis settings
- `POST /api/risk/config` — update thresholds and weights at runtime
- `GET /api/risk/current` — risk score, level, trend, confidence, reasons, factor descriptions, horizon, disclaimer
- `GET /api/analytics/current` — analytics for all zones, detected bottlenecks/counterflow, overall summary
- `GET /api/analytics/zones/{zoneId}` — detailed zone analytics + historical timeline
- `GET /api/alerts` — active alerts with persistence and deduplication

## Repo
- Git initialized, pushed to https://github.com/sarthakksingh/CrowdShield.git

## Next Phase
**Phase 8 — Operator Dashboard Integration** (or Phase 9 — Mobile App)

## Explicitly Out of Scope For Now
Dashboard, Android app, YOLO/vision integration, database, auth, Kafka, Kubernetes.

## Hackathon Priority (2-day window)
Must-ship: Phase 8 → 9 → 13
Simplify if tight: Phase 5 (linear trend only), Phase 6 (fold into Phase 9 minimal), Phase 10, 11, 12