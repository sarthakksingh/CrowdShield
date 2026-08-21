# CrowdShield Architecture (Phase 0)

## 1) Recommended monorepo structure

```text
crowdshield/
  backend/          Spring Boot REST/SSE API
  vision-service/   Python video and metric extraction
  dashboard/        React authority dashboard
  mobile/           Kotlin Android citizen app
  demo-data/        venue graph and scripted scenarios
  docs/             architecture and planning docs
```

## 2) Build tool choices

| Component | Stack | Build/Run Tooling | Why this choice |
|---|---|---|---|
| backend | Spring Boot (Java/Kotlin compatible) | **Gradle Wrapper** (`./gradlew`) | Fast local setup, widely used with Spring and Android teams |
| vision-service | Python 3.11 | **pip + pinned `requirements.txt`** | Minimal moving parts; deterministic installs for demo reliability |
| dashboard | React + TypeScript + Vite | **npm** | Lowest friction for quick prototype and predictable dev server/build |
| mobile | Kotlin Android | **Gradle Wrapper** | Native Android standard and team familiarity |
| demo-data | JSON/CSV/scripts | Python scripts + static files | Reproducible scenario playback |

Notes:
- Keep everything CPU-friendly; no GPU assumption.
- Keep external LLM/API calls optional and wrapped with timeout + fallback.

## 3) Service boundaries

## backend (Spring Boot)
Owns:
- Event state (current event timeline and status)
- Venue graph (zones, edges, capacities)
- Zone metrics state store (latest + short history)
- Risk scoring (explainable weighted formula)
- Forecasting (short-horizon projection, e.g., 2-5 minutes)
- Recommendation generation
- Simulation endpoint for "what-if" interventions
- Alert lifecycle (create, dedupe, acknowledge, expire)
- SSE stream for dashboard/mobile updates

Does **not**:
- Run heavy computer vision directly
- Depend on live camera for core demo loop

## vision-service (Python)
Owns:
- Reading recorded video files
- Optional person detection/tracking (pluggable)
- Optical-flow / motion intensity extraction
- Zone-level metric output per timestamp
- Data quality flags (no feed, low FPS, missing window)

Emits to backend:
- Timestamped zone metrics payloads (HTTP push or polled file feed)

## dashboard (React)
Owns:
- Live venue map and zone overlays
- Heatmap + risk zones
- Explainable risk panel (why score changed)
- Recommendation feed
- Intervention simulator UI (calls backend simulation API)
- Operator action logging (acknowledge/apply recommendation)

## mobile (Kotlin Android)
Owns:
- Location-aware safety alerts
- Congestion warnings
- Safest route display
- Incident reporting
- Emergency information screen
- Offline cache for last known advisory + route when backend unavailable

## 4) End-to-end data flow

1. **OBSERVE**: vision-service emits zone metrics (or replayed demo-data emits metrics directly).
2. **UNDERSTAND**: backend validates metrics, fills gaps, updates zone state.
3. **PREDICT**: backend computes risk + short-term forecast with confidence.
4. **SIMULATE**: dashboard triggers simulation (e.g., "close Gate B", "open corridor C").
5. **RECOMMEND**: backend outputs ranked interventions with impact estimate.
6. **ACT**: operator chooses action; backend records action state.
7. **VERIFY**: backend tracks post-action metrics deltas and emits outcome.

## 5) Edge-case handling principles

- No camera feed: mark feed `UNAVAILABLE`; switch to degraded mode using last valid metrics + confidence drop.
- Low FPS: include `quality.lowFps=true`; widen smoothing window; lower confidence.
- Missing metrics: zone marked `STALE`; risk uses neighbor/temporal interpolation with explicit reason flags.
- Repeated alerts: dedupe by `(zoneId, alertType, severityBand, timeBucket)`.
- Risk flickering: hysteresis + minimum hold duration before severity downgrade.
- Backend unavailable: dashboard/mobile show "stale data" banner and last sync time.
- Mobile offline: show cached safest route + emergency info, disable incident submission queue flush until online.
- Invalid incident reports: strict validation with descriptive 4xx errors.

## 6) Simplification strategy (for hackathon fit)

- Start with deterministic prerecorded scenarios first; make live feed optional.
- Use explainable formula-based risk baseline before any advanced ML model.
- Keep persistence lightweight (single relational DB) with short event retention.
- Skip auth/Kafka/Kubernetes in Phase 1; keep deployment single-host demo ready.
