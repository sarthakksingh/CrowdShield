# CrowdShield — Live Demo Script & Video Proof Guide

A deterministic, timed walkthrough script for recording hackathon video proof and live presentations.

---

## Beat 0: Pre-Flight Dry-Run Checklist

Run this quick checklist before every recording take to ensure a 100% reproducible demo:

1. **Backend Check**:
   - Ensure `http://localhost:8080/api/playback` returns HTTP 200:
     ```bash
     curl -s http://localhost:8080/api/playback
     ```
2. **Dashboard Check**:
   - Ensure Vite is running on `http://localhost:5173`.
3. **State Reset**:
   - If resetting between takes, click the **Reset** button in the dashboard playback bar (or select `1. Normal Flow (Baseline)` from the dropdown) to return the timeline to `t=00:00`.
4. **Browser Setup**:
   - Open browser at `100%` zoom on `http://localhost:5173`.
   - Ensure full viewport visibility showing both the Venue Overview, Risk Assessment, Recommendations, and Simulation Sandbox.

---

## Timed Walkthrough Beats

### Beat 1: Cold Open (10–15s)
- **Action**: Launch the dashboard via `start-dashboard.bat` (or refresh `http://localhost:5173`).
- **Narrative**:
  > "Welcome to CrowdShield — an intelligent crowd safety intelligence and decision-support system designed to prevent stampedes and crush incidents before they happen. Notice our prototype safety disclaimer banner at the top, emphasizing that CrowdShield operates as an advisory decision-support platform for authorized operations personnel."

---

### Beat 2: Normal State Baseline (20–30s)
- **Action**: Verify scenario dropdown is set to `1. Normal Flow (Baseline)` at `00:00`.
- **Visuals to Point Out**:
  - Venue Zone Overview cards all display **LOW** risk with green borders.
  - Overall Risk Index is nominal (~0.20), STABLE trend.
  - Recommendation Panel displays: *"No Urgent Interventions Required — Crowd flow is laminar."*
  - Alerts feed confirms *"All Zones Nominal"*.
- **Narrative**:
  > "During standard operations, all sectors — Entry A, Corridor 1, Open Yard, and Exit East — operate under laminar, nominal flow. Densities remain around 1.0 to 1.5 persons/m², and the system raises no urgent alarms."

---

### Beat 3: Risk Build-up (30–45s)
- **Action**: In the scenario dropdown, select `2. Entry Bottleneck & Choke` and click **Play** (or seek to `00:40`).
- **Visuals to Point Out**:
  - Live timeline counter increments live via Server-Sent Events (SSE).
  - Inflow at Entry A reaches 128 persons/min while Corridor 1 outflow drops to 59 persons/min.
  - Corridor 1 density climbs rapidly from 1.5 to 3.8+ persons/m².
  - Net flow indicator turns orange (`+69/m`), and queue length accumulates.
- **Narrative**:
  > "Now, we switch to an entry bottleneck scenario. As attendees surge through Entry Gate A, Transit Corridor 1 begins to choke. Notice how the dashboard updates live in real-time over SSE without manual refreshing — density surges past 3.8 persons/m² and queue accumulation spikes."

---

### Beat 4: Critical Escalation & Explainability (30–45s)
- **Action**: Corridor 1 turns red (**CRITICAL (88%)**). Click on the **Transit Corridor 1** card to open Sector Diagnostics.
- **Visuals to Point Out**:
  - **BOTTLENECK CHOKE** badge appears with explainability reasons:
    - `inflow_exceeds_outflow`
    - `high_density_accumulation`
    - `severe_speed_drop`
    - `growing_queue`
  - Speed drop exceeds 45%.
  - Factor Breakdown chart in Risk Assessment shows `density_pressure` and `bottleneck_pressure` contributing the majority of the risk score.
- **Narrative**:
  > "Corridor 1 has escalated to CRITICAL. CrowdShield doesn't just display a number — it explains WHY. The diagnostic panel breaks down the exact triggers: inflow exceeds outflow, walking speed has degraded by over 45%, and density pressure has reached dangerous compression thresholds."

---

### Beat 5: Time-to-Critical Horizon (10–15s)
- **Action**: Point cursor to the **TIME-TO-CRITICAL HORIZON** card in the Risk Summary panel.
- **Visuals to Point Out**:
  - Horizon reads `"immediate (< 1 min)"` or `"1-2 min"`.
- **Narrative**:
  > "Notice our dynamic Time-to-Critical Horizon estimate: operations teams are alerted that critical crush conditions are imminent within 1 to 2 minutes if current inflow trends continue."

---

### Beat 6: Actionable Advisory Recommendations (30–40s)
- **Action**: Pan to the **Actionable Advisory Recommendations** panel.
- **Visuals to Point Out**:
  - Prioritized list of advisories:
    - `#1 RESTRICT_GATE` on `z-entry-a`: *"Meters incoming crowd rate to prevent hazardous crush pressure."*
    - `#2 OPEN_ROUTE` on `z-corridor-1`: *"Provides immediate bypass relief into open sectors."*
    - `#3 DEPLOY_PERSONNEL` on `z-corridor-1`: *"Dispatches safety stewards for physical queue management."*
    - `#4 BROADCAST_ALERT`: Suggested public address script.
- **Narrative**:
  > "The Intervention Engine automatically generates ranked, concrete actions. Notice the advisory language: CrowdShield recommends restricting Gate A turnstiles and opening bypass routes. Each suggestion includes grounded reasons, expected qualitative impact, and confidence scores — strictly decision-support for human operators."

---

### Beat 7: Simulation Sandbox & Transferred Risk (30–40s)
- **Action**: On recommendation `#1` or in the Simulation Sandbox, click **Test in Simulator** / **Run Before/After Simulation Analysis**.
- **Visuals to Point Out**:
  - Projected Peak Risk drops from `0.88` to `0.55` (**-33% Risk Delta**).
  - Projected Verdict: **IMPROVED**.
  - Transferred Upstream Risk Warning callout: *"Restricting entry at z-entry-a shifts queue accumulation upstream to entry holding zone."*
- **Narrative**:
  > "Before taking action, operators can test interventions in our stateless Simulation Sandbox. Simulating a gate restriction combined with route opening projects a 33% drop in corridor peak risk. Crucially, the engine warns of transferred risk: restricting Gate A relieves the corridor but will build secondary queues in the exterior holding area, prompting steward deployment there."

---

### Beat 8: Post-Intervention Recovery (15–20s)
- **Action**: Select `4. Post-Intervention Recovery` from the scenario dropdown.
- **Visuals to Point Out**:
  - Outflow exceeds inflow.
  - Density drops back below 2.0/m², queue shrinks to zero.
  - Risk indices and badges return to **LOW / MODERATE**.
- **Narrative**:
  > "Once interventions are enacted, we observe post-intervention recovery. As crowd volume disperses toward Exit East, densities normalize and the venue returns to safe operating status."

---

### Beat 9: Close & Tech Stack Summary (10s)
- **Action**: Point cursor back to the safety disclaimer banner and tech badges in the header.
- **Narrative**:
  > "CrowdShield is powered by a high-performance Java Spring Boot backend with real-time heuristic risk scoring and SSE streaming, coupled with a React, TypeScript, and Vite operations dashboard. Thank you."

---

## 3x Repeatability Test Summary
- Scenario switching between `s1` → `s2` → `s4` resets cleanly with zero residual state pollution.
- Live SSE stream updates without page refresh.
- Simulation runs statelessly without altering live playback metrics.
