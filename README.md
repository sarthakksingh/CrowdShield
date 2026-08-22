# CrowdShield

**CrowdShield** is an AI-powered crowd safety intelligence and real-time decision-support platform designed to prevent stampedes and crush disasters before they happen. By continuously analyzing crowd density, inflow/outflow balance, movement instability, and counterflow dynamics, CrowdShield forecasts short-horizon risks, generates explainable decision advisories for venue authorities, runs what-if intervention simulations, and broadcasts real-time safety telemetry to both authority dashboards and citizen mobile devices.

Product loop: **OBSERVE -> UNDERSTAND -> PREDICT -> SIMULATE -> RECOMMEND -> ACT -> VERIFY**.

---

## 🌐 Live Demo

- **Authority Dashboard**: [https://crowd-shield-three.vercel.app](https://crowd-shield-three.vercel.app)
- **Backend API**: [https://crowdshield-backend-iy6g.onrender.com](https://crowdshield-backend-iy6g.onrender.com)

> [!NOTE]
> The backend is hosted on a free-tier instance on Render and may take 30–60 seconds to spin up on the first request if spinning down from inactivity.

---

## 📱 Mobile App (Citizen Safety App)

The repository includes a native Android app located in `mobile/app`:
- **Stack**: Kotlin, Jetpack Compose, Material3, Retrofit 2, OkHttp, Coroutines.
- **3 Core Screens**:
  1. **Live Risk Screen**: Live venue safety index, time-to-critical horizon estimate, and color-coded sector safety cards with real-time polling.
  2. **Incident Alerts Feed**: Active incident stream, severity badges, timestamps, and nominal empty states.
  3. **Ground Incident Report**: Direct citizen dispatch with sector dropdown, category/severity selectors, and submission to venue operations triage.
- **Backend Connection**: Configured out-of-the-box to connect to the deployed Render production backend.
- **Build & Run**: Open the `mobile/` directory in **Android Studio** and run on any connected physical Android device or emulator (or build APK via `cd mobile && .\gradlew.bat assembleDebug`).

---

## 🚀 Completed Phases

- **Phase 0 — Repository Audit & Architecture Baseline**: System architecture, API contracts, ethics, and demo strategy.
- **Phase 1 — Backend Vertical Slice**: Spring Boot REST/SSE foundation with in-memory deterministic data loading.
- **Phase 2 — Scenario Playback & SSE Streaming**: Multi-scenario playback engine (`s1`–`s4`) with real-time SSE broadcasts.
- **Phase 3 — Crowd Analytics Engine**: Multi-factor bottleneck choke detection, net accumulation pressure, speed drop %, and counterflow turbulence analysis.
- **Phase 4 — Explainable Risk Engine**: Configurable weighted risk scoring with hysteresis damping and persistence verification.
- **Phase 5 — Short-Horizon Prediction**: Statistical forecasting via Linear Regression and Holt's Linear Exponential Smoothing across 30s/60s/180s/300s horizons.
- **Phase 7 — Actionable Intervention Engine**: Grounded, prioritized decision advisories (`RESTRICT_GATE`, `OPEN_ROUTE`, `DEPLOY_PERSONNEL`, `BROADCAST_ALERT`).
- **Phase 8 — Stateless Simulation Sandbox**: What-if intervention testing projecting risk deltas and warning of transferred upstream queue risk.
- **Phase 9 — Authority Dashboard**: High-contrast tactical React + TypeScript + Vite operations dashboard with interactive playback, diagnostics, and sandbox.
- **Phase 10 — Mobile Citizen App**: Scoped 3-screen native Android application with live telemetry, alerts, and incident reporting.
- **Phase 13 — Presentation & Demo Readiness**: One-click startup scripts, deterministic timed demo script, and complete offline/online test coverage.

---

## 💻 Local Development

### One-Click Startup Scripts
- **Backend**:
  - Windows: Run `start-backend.bat`
  - Linux / macOS: Run `./start-backend.sh`
- **Dashboard**:
  - Windows: Run `start-dashboard.bat`
  - Linux / macOS: Run `./start-dashboard.sh`

### Manual Startup

#### Backend (Spring Boot + Java 21)
```bash
cd backend
./gradlew bootRun
```
Endpoints available at `http://localhost:8080`:
- `GET http://localhost:8080/api/risk/current`
- `GET http://localhost:8080/api/analytics/current`
- `GET http://localhost:8080/api/forecast/current`
- `GET http://localhost:8080/api/recommendations/current`
- `GET http://localhost:8080/api/alerts`
- `GET http://localhost:8080/api/stream` (SSE)

#### Dashboard (React + TypeScript + Vite)
```bash
cd dashboard
npm install
npm run dev
```
Dashboard will be available at `http://localhost:5173` (automatically proxies `/api` requests to backend).

---

## 📦 Docker Containerization

A multi-stage production Dockerfile is provided at `backend/Dockerfile`:
```bash
docker build -t crowdshield-backend:latest ./backend
docker run -d -p 8080:8080 crowdshield-backend:latest
```

---

## 📚 Documentation

- [System Architecture & Data Flow](docs/ARCHITECTURE.md)
- [Timed Demo Script & Video Proof Guide](docs/DEMO_SCRIPT.md)
- [API Specifications & Contracts](docs/API_CONTRACTS.md)
- [Privacy & Ethical Safeguards](docs/PRIVACY_AND_ETHICS.md)
- [Current Status & Verification Summary](docs/CURRENT_STATUS.md)
