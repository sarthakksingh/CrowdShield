# CrowdShield — System Architecture & Data Flow

CrowdShield is a real-time crowd safety intelligence and decision-support platform designed to monitor venue congestion, detect physical flow anomalies, evaluate explainable risk scores, and simulate proactive crowd interventions.

---

## 1. High-Level Architecture Diagram

```text
+-------------------------------------------------------------------------------+
|                             CROWDSHIELD SYSTEM                                |
+-------------------------------------------------------------------------------+

  [ Prerecorded Scenarios / Vision Feed ]
                    |
                    v
    +-----------------------------------------------+
    |        Spring Boot Backend (Port 8080)        |
    +-----------------------------------------------+
    |  - ScenarioPlaybackService (Timeline/Frames)  |
    |  - CrowdAnalyticsService (Flow/Bottlenecks)   |
    |  - RiskScoringService (Weighted Formula)      |
    |  - RiskStateService (Hysteresis Damping)      |
    |  - AlertService (Persistence Filtered)        |
    |  - InterventionEngineService (Advisors)       |
    |  - SimulationService (Stateless Projections)  |
    +-----------------------------------------------+
           |                            |
    (REST API: HTTP 200)       (SSE Stream: /api/stream)
           |                            |
           +--------------+-------------+
                          |
                          v
    +-----------------------------------------------+
    |      Authority Dashboard (Port 5173)          |
    +-----------------------------------------------+
    |  - PlaybackBar (Controls & Timeline Slider)   |
    |  - VenueOverview (Traffic-Light Zone Grid)    |
    |  - RiskSummaryPanel (Index, Horizon, Drivers) |
    |  - ZoneDetailPanel (Sector Diagnostics)       |
    |  - RecommendationPanel (Advisory Guidance)    |
    |  - SimulationSandbox (Before/After Tester)    |
    |  - AlertsFeed (Deduplicated Incident Stream)  |
    +-----------------------------------------------+
```

---

## 2. End-to-End Data Flow

```text
+----------+      1. Stream/Seek      +-------------------------+
| Playback | -----------------------> | ScenarioPlaybackService |
+----------+                          +-------------------------+
                                                   |
                                                   | 2. Zone Metrics Frame
                                                   v
+------------------------+            +-------------------------+
|  CrowdAnalyticsService | <--------- |    RiskScoringService   |
+------------------------+            +-------------------------+
| • Density Trends       |                         |
| • Net Pressure         |            3. Risk Level & Factors
| • Speed Drop %         |                         |
| • Bottleneck Detectors |                         v
| • Counterflow Conflict |            +-------------------------+
+------------------------+            |     RiskStateService    |
            |                         +-------------------------+
            |                         | • Hysteresis Downgrade  |
            |                         | • Hold Window Damping   |
            |                         +-------------------------+
            |                                      |
            | 4. Diagnostics & Risk Snapshot       v
            |                         +-------------------------+
            +-----------------------> |      AlertService       |
            |                         +-------------------------+
            |                         | • Persistence (N >= 2)  |
            |                         | • Alert Deduplication   |
            |                         +-------------------------+
            |                                      |
            v                                      v
+---------------------------------------------------------------+
|                   InterventionEngineService                   |
+---------------------------------------------------------------+
| • GateAdvisor      (RESTRICT_GATE, OPEN_EXIT)                 |
| • RouteAdvisor     (OPEN_ROUTE, REDIRECT_INFLOW)              |
| • PersonnelAdvisor (DEPLOY_PERSONNEL)                         |
| • Announcement     (BROADCAST_ALERT)                          |
+---------------------------------------------------------------+
            |
            | 5. Interactive Simulation Request
            v
+---------------------------------------------------------------+
|                       SimulationService                       |
+---------------------------------------------------------------+
| • Clones Current State (Zero Live Mutation)                   |
| • Recalculates Capacity & Flow Dynamics                       |
| • Evaluates Projected Risk via Stateless Scoring Engine       |
| • Detects Transferred Upstream Queue Pressure                 |
+---------------------------------------------------------------+
            |
            | 6. REST / SSE Delivery
            v
+---------------------------------------------------------------+
|                  React Authority Dashboard                    |
+---------------------------------------------------------------+
```

---

## 3. Backend Service Responsibilities

### 1. `ScenarioPlaybackService`
- Manages playback state across 4 deterministic scenarios (`s1_normal_flow`, `s2_entry_bottleneck`, `s3_counterflow_panic`, `s4_post_intervention_recovery`).
- Handles `play`, `pause`, `seek`, `load`, and `reset` lifecycle actions.
- Broadcasts real-time events over Server-Sent Events (`/api/stream`).

### 2. `CrowdAnalyticsService`
- Computes density trends (persons/m²), net accumulation pressure ($\text{inflow} - \text{outflow}$), speed degradation percentage, queue growth, and movement instability.
- Identifies **Bottlenecks** with multi-factor explainability (`inflow_exceeds_outflow`, `high_density_accumulation`, `severe_speed_drop`, `growing_queue`).
- Identifies **Counterflow Conflicts** and opposing movement turbulence.

### 3. `RiskScoringService` & `RiskStateService`
- Evaluates multi-factor risk scores based on configurable weights and thresholds in `application.yml`.
- Provides human-readable factor contribution percentages and dynamic time-to-critical horizon estimates.
- **Hysteresis Damping**: Requires sustained improvement over consecutive frames to downgrade severity, eliminating frame-to-frame risk flickering.

### 4. `AlertService`
- **Persistence Verification**: Suppresses single-frame noise spikes; requires elevated risk for $\ge 2$ consecutive frames before raising alerts.
- Deduplicates alerts across zones and time buckets.

### 5. `InterventionEngineService`
- Aggregates decision-support advisories from `GateAdvisor`, `RouteAdvisor`, `PersonnelAdvisor`, and `AnnouncementAdvisor`.
- Ranks candidate recommendations by urgency, impact, and confidence using strictly advisory phrasing.

### 6. `SimulationService`
- Executes what-if intervention testing on an isolated copy of current zone metrics.
- Flags transferred upstream risk (e.g. gate restrictions shifting queues upstream to exterior holding zones).
- Guarantees zero mutation to live playback and risk state.

---

## 4. API Endpoints Summary

| Method | Route | Description |
|---|---|---|
| `GET` | `/api/events/current` | Active event metadata and venue identifier |
| `GET` | `/api/playback` | Current playback state (offset, scenario, playing status) |
| `POST` | `/api/playback/play` | Starts scenario playback |
| `POST` | `/api/playback/pause` | Pauses scenario playback |
| `POST` | `/api/playback/seek` | Seeks to a specific offset in seconds |
| `POST` | `/api/playback/load` | Loads a target scenario |
| `GET` | `/api/risk/current` | Real-time overall and per-zone risk assessments |
| `GET` | `/api/risk/config` | Runtime inspection of risk thresholds and weights |
| `POST` | `/api/risk/config` | Updates thresholds and weights at runtime |
| `GET` | `/api/analytics/current` | Venue-wide analytics, bottlenecks, and counterflow |
| `GET` | `/api/analytics/zones/{zoneId}` | Deep sector diagnostics and historical timeline |
| `GET` | `/api/alerts` | Active deduplicated incident alerts |
| `GET` | `/api/recommendations/current` | Prioritized, explainable decision-support advisories |
| `POST` | `/api/simulations` | Stateless before/after intervention risk projection |
| `POST` | `/api/incidents` | Ground citizen incident report dispatch |
| `GET` | `/api/stream` | Server-Sent Events (SSE) live update channel |

---

## 5. Deployment Architecture

CrowdShield is deployed across a decoupled, cloud-native infrastructure with full HTTPS and CORS security:

```text
+-----------------------------------------------------------------------------------+
|                            LIVE DEPLOYED TOPOLOGY                                 |
+-----------------------------------------------------------------------------------+

   +---------------------------------------+
   |   Authority Dashboard (Vercel)        |
   |   https://crowd-shield-three.vercel.app |
   +---------------------------------------+
                      |
                      | HTTPS / SSE (VITE_API_BASE_URL)
                      v
   +---------------------------------------+      HTTPS / JSON       +------------------------------------+
   |   Backend Service (Render)            | <---------------------- |   Citizen Mobile App (Android)     |
   |   https://crowdshield-backend-iy6g    |                         |   Kotlin + Jetpack Compose         |
   |   .onrender.com                       |                         |   (mobile/app)                     |
   +---------------------------------------+                         +------------------------------------+
                      |
           [ Multi-Stage Docker ]
           - Builder: gradle:8.8-jdk21-alpine
           - Runtime: eclipse-temurin:21-jre-alpine (non-root)
           - Dynamic Port: ${PORT:8080}
           - Data Dir: ${DEMO_DATA_DIR:/demo-data}
```

1. **Backend Service (Render)**:
   - Built using a secure, multi-stage [`backend/Dockerfile`](../backend/Dockerfile) (`gradle:8.8-jdk21-alpine` builder and `eclipse-temurin:21-jre-alpine` runtime).
   - Runs as an unprivileged system user `crowdshield:crowdshield`.
   - Supports dynamic port binding (`${PORT:8080}`) and environment-driven data directory resolution (`${DEMO_DATA_DIR:/demo-data}`).
2. **Authority Dashboard (Vercel)**:
   - High-performance React SPA built with Vite and TypeScript.
   - Configured with `VITE_API_BASE_URL=https://crowdshield-backend-iy6g.onrender.com` to communicate securely across origins.
3. **Citizen Mobile App (Android)**:
   - Native Android client in `mobile/app` using Jetpack Compose and Retrofit 2.
   - Points directly to `https://crowdshield-backend-iy6g.onrender.com/` for real-time risk telemetry, alert notifications, and crowd dispatch triage.
