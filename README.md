# CrowdShield

CrowdShield is a TechNova Round 2 hackathon prototype for **early warning and decision support to prevent crowd stampedes**.

Product loop: **OBSERVE -> UNDERSTAND -> PREDICT -> SIMULATE -> RECOMMEND -> ACT -> VERIFY**.

## Phase 0 status (accepted)

## Phase 1 status (backend vertical slice in progress)

Current scaffold:
- `backend/` Spring Boot + Gradle backend with in-memory demo-data loading and core API endpoints.
- `demo-data/` deterministic venue/scenario JSON for sample bottleneck run.
- `dashboard/`, `mobile/`, `vision-service/` placeholder READMEs only (implementation deferred).

Phase 0 deliverables created in `docs/`:
- `docs/ARCHITECTURE.md`
- `docs/PHASE_PLAN.md`
- `docs/API_CONTRACTS.md`
- `docs/DEMO_STRATEGY.md`
- `docs/PRIVACY_AND_ETHICS.md`

## Recommended monorepo shape

```text
crowdshield/
  backend/          Spring Boot REST/SSE API
  vision-service/   Python video and metric extraction
  dashboard/        React authority dashboard
  mobile/           Kotlin Android citizen app
  demo-data/        venue graph and scripted scenarios
  docs/             architecture and planning docs
```

## Backend quick start

From `backend/`:

```bash
./gradlew bootRun
```

Then open:
- `GET http://localhost:8080/api/risk/current`
- `GET http://localhost:8080/api/alerts`
- `GET http://localhost:8080/api/stream`

See `docs/ARCHITECTURE.md` and `docs/PHASE_PLAN.md` for scope and next steps.
