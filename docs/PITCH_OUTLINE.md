# CrowdShield — Hackathon Pitch Outline

A structured outline for presenting CrowdShield in a 3 to 5-minute hackathon demo pitch.

---

## 1. Problem
- **Catastrophic Crush Risks**: Mass gatherings (concerts, religious festivals, stadiums) suffer recurring crowd stampedes and crush disasters caused by unmanaged bottlenecks and counterflow collisions.
- **Delayed & Reactive Response**: Current venue monitoring relies on human visual observation or basic CCTV counts, which only recognize danger after physical crush conditions have already occurred.
- **Black-Box Alerting**: Traditional threshold alarms fail to explain *why* risk is rising or warn if an intervention will merely transfer congestion to adjacent zones.

---

## 2. Solution
- **CrowdShield**: A real-time crowd safety intelligence and decision-support platform that detects pre-crush dynamics, explains contributing risk factors, and projects intervention outcomes before physical action is taken.
- **Multi-Factor Flow Analytics**: Continuous tracking of density pressure, net accumulation rate ($\text{inflow} - \text{outflow}$), speed degradation %, movement instability, and opposing counterflow turbulence.
- **Actionable Decision-Support**: Generates ranked, grounded advisory recommendations (gate metering, route opening, steward deployment, PA announcements) paired with an interactive **Simulation Sandbox** that detects transferred risk.

---

## 3. Tech Stack
- **Backend Core**: Java 21 / Spring Boot 3 with high-throughput in-memory scenario playback and stateful hysteresis damping.
- **Real-Time Delivery**: Server-Sent Events (SSE) streaming live metric frames, risk updates, and alerts with zero polling overhead.
- **Frontend Dashboard**: React 18, TypeScript, and Vite with custom high-contrast dark-mode tactical UI components.
- **Zero Heavy Infrastructure Dependency**: Fully self-contained, CPU-friendly, deterministic, and runnable offline without external cloud dependencies.

---

## 4. Live Demo Flow
1. **Cold Open**: Launch dashboard with live disclaimer banner highlighting decision-support architecture.
2. **Normal State**: `s1_normal_flow` baseline demonstrating nominal green zones and zero urgent alarms.
3. **Escalation & Diagnostics**: `s2_entry_bottleneck` progression showing live density surge over SSE, corridor choke, and explainability breakdown (`inflow_exceeds_outflow`, `severe_speed_drop`).
4. **Advisory Recommendations**: Review prioritized crowd management actions with confidence and expected impact.
5. **Simulation Sandbox**: Simulate Gate A restriction and Route C opening, verifying a 33% projected peak risk reduction and highlighting upstream transferred risk warnings.
6. **Recovery**: `s4_post_intervention_recovery` demonstrating crowd dispersal and venue normalization.

---

## 5. Impact & Value
- **Early Incident Prevention**: Identifies dangerous compression 2–5 minutes before human operators can visually detect critical choke points.
- **Explainable Operations**: Empowers security chiefs and municipal authorities with grounded diagnostic metrics rather than arbitrary risk scores.
- **Safe Intervention Testing**: Prevents unintended secondary crushes by simulating flow redistribution and transferred queue pressure in a sandbox.

---

## 6. Roadmap
- **Phase 1 (Immediate)**: Integration with edge CCTV camera feeds via lightweight YOLO person detection and optical flow.
- **Phase 2**: Citizen-facing mobile app providing location-aware safe egress routing and emergency broadcast notifications.
- **Phase 3**: Graph-based digital twin venue modeling with multi-venue federated monitoring for city-wide event management.
