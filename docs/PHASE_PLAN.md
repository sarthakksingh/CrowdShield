# CrowdShield Phase Plan (Phase 0 -> Phase 1)

## Phase 0 outcome (complete)

- Repository audited and documented.
- Target architecture and service boundaries defined.
- API contracts drafted.
- Deterministic demo-data strategy defined.
- Privacy and ethics guardrails defined.

## 10-day delivery plan (2026-08-14 to 2026-08-23)

## Day 1-2: Monorepo scaffolding + contracts lock
- Initialize git repository and base monorepo folders.
- Scaffold Spring Boot backend skeleton with API stubs.
- Scaffold React dashboard, Android app, Python vision-service skeleton.
- Freeze v1 API fields in `docs/API_CONTRACTS.md`.

## Day 3-4: Core backend state and risk pipeline
- Implement venue graph loader and in-memory event state.
- Implement zone metrics ingest and validation.
- Implement explainable risk scoring + short-term forecast baseline.
- Implement alert dedupe + hysteresis logic.

## Day 5-6: UI surfaces (dashboard + mobile baseline)
- Dashboard: map/zones, risk panel, alert list, recommendation panel.
- Mobile: alert feed, safest route screen, emergency info, incident submit form.
- SSE client wiring for real-time updates.

## Day 7: Simulation + recommendation loop
- Implement simulation endpoint and simple intervention effects model.
- Show baseline vs projected risk and recommended actions in dashboard.

## Day 8: Demo-data integration and replay controls
- Add scenario playback engine from `demo-data/`.
- Add deterministic timeline controls (play/pause/seek/reset).
- Add fallback mode when video feed unavailable.

## Day 9: Hardening and edge-case UX
- Handle low FPS/missing metrics/backend unavailable/offline mobile cases.
- Add data quality badges and stale-data banners.
- Validate invalid incident report handling.

## Day 10: Final rehearsal and packaging
- End-to-end rehearsal scripts and operator runbook.
- Capture demo flow with fixed scenario sequence.
- Final docs polish and known-limitations checklist.

## Exact next implementation steps for Phase 1

1. Initialize git in repository root and create the six top-level folders from architecture doc.
2. Generate backend Spring Boot project with endpoints:
   - `GET /api/events/current`
   - `GET /api/venue`
   - `GET /api/zones`
   - `GET /api/risk/current`
   - `GET /api/alerts`
   - `POST /api/simulations`
   - `POST /api/incidents`
   - `GET /api/routes/safest`
   - `GET /api/stream`
3. Add backend seed loaders for `demo-data/venue_graph.json` and `demo-data/scenarios/s2_entry_bottleneck.json`.
4. Implement first risk scoring module with explainable factor breakdown and confidence output.
5. Implement alert dedupe key and risk hysteresis hold window.
6. Build minimal dashboard page that renders venue zones, risk badges, and live alert feed from SSE.
7. Build minimal mobile screen set:
   - alert list
   - safest route
   - incident submit form with validation
8. Build vision-service replay mode that emits zone metrics from scenario files (video optional).
9. Run one deterministic scenario end-to-end and capture expected outputs as baseline fixtures.
10. Freeze Phase 1 demo checklist and move to Phase 2 improvements only after baseline loop is stable.

## Explicitly out of scope for Phase 1

- Authentication and role management
- Kubernetes and distributed orchestration
- Kafka/event bus infrastructure
- GPU-dependent ML pipelines
- Facial recognition or identity-level tracking
