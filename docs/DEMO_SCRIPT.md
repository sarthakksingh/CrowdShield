# CrowdShield — Live Demo Script & Video Proof Guide

A deterministic, timed walkthrough script for recording hackathon video proof and live presentations using the deployed production system.

---

## 🌐 Live Demo Target Environment

- **Live Authority Dashboard (Primary)**: [https://crowd-shield-three.vercel.app](https://crowd-shield-three.vercel.app)
- **Live Backend API**: [https://crowdshield-backend-iy6g.onrender.com](https://crowdshield-backend-iy6g.onrender.com)
- **Local Fallback**: `http://localhost:5173` (via `start-dashboard.bat`) and `http://localhost:8080` (via `start-backend.bat`).

---

## Beat 0: Pre-Flight Dry-Run Checklist

Run this quick checklist before every recording take to ensure a 100% reproducible demo:

1. **Backend Health Check**:
   - Ensure the live backend endpoint returns HTTP 200:
     ```bash
     curl -s https://crowdshield-backend-iy6g.onrender.com/api/playback
     ```
     *(If spinning up from sleep on Render, wait ~30-45s for the initial cold start)*
2. **Dashboard Access**:
   - Open [https://crowd-shield-three.vercel.app](https://crowd-shield-three.vercel.app) in your browser.
3. **State Reset**:
   - If resetting between takes, click the **Reset** button in the dashboard playback bar (or select `1. Normal Flow (Baseline)` from the dropdown) to return the timeline to `t=00:00`.
4. **Browser Setup**:
   - Open browser at `100%` zoom.
   - Ensure full viewport visibility showing the Venue Overview, Risk Assessment, Recommendations, and Simulation Sandbox.

---

## Timed Walkthrough Beats

### Beat 1: Cold Open (10–15s)
- **Action**: Open [https://crowd-shield-three.vercel.app](https://crowd-shield-three.vercel.app) (or local fallback `http://localhost:5173`).
- **Narrative**:
  > "Welcome to CrowdShield — an intelligent crowd safety intelligence and decision-support system designed to prevent stampedes and crush incidents before they happen. Notice our prototype safety disclaimer banner at the top, emphasizing that CrowdShield operates as an advisory decision-support platform for authorized operations personnel."

---

### Beat 2: Normal State Baseline (20–30s)
- **Action**: Verify scenario dropdown is set to `1. Normal Flow (Baseline)` at `00:00`.
- **Visuals to Point Out**:
  - Venue Zone Overview cards all display **LOW** risk with green borders.
  - Overall Risk Index is nominal (~0.20), STABLE trend.
  - Statistical Projection card indicates stable baseline projection across 60s and 180s horizons.
  - Recommendation Panel displays: *"No Urgent Interventions Required — Crowd flow is laminar."*
  - Alerts feed confirms *"All Zones Nominal"*.
- **Narrative**:
  > "During standard operations, all sectors — Entry A, Corridor 1, Open Yard, and Exit East — operate under laminar, nominal flow. Densities remain around 1.0 to 1.5 persons/m², and the system raises no urgent alarms."

---

### Beat 3: Risk Build-up (30–45s)
- **Action**: In the scenario dropdown, select `2. Entry Bottleneck & Choke` and click **Play** (or seek to `00:40`).
- **Visuals to Point Out**:
  - Live timeline counter increments smoothly via Server-Sent Events (SSE) and client-side interpolation.
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

### Beat 5: Short-Horizon Prediction & Time-to-Critical (15–20s)
- **Action**: Point cursor to the **Statistical Projection** and **Time-to-Critical Horizon** cards in the Risk Assessment panel.
- **Visuals to Point Out**:
  - Horizon reads `"immediate (< 1 min)"` or `"1-2 min"`.
  - Statistical forecast projects critical risk at +60s and +180s horizons with high confidence and model consensus.
- **Narrative**:
  > "Our dual forecasting engine combines linear trend regression and Holt's exponential smoothing to project risk 30 seconds to 5 minutes into the future. Operators are given early warning before physical disaster occurs."

---

### Beat 6: Actionable Advisory Recommendations (30–40s)
- **Action**: Pan to the **Actionable Advisory Recommendations** panel.
- **Visuals to Point Out**:
  - Prioritized list of advisories:
    - `#1 RESTRICT_GATE` on `z-entry-a`: *"Meters incoming crowd rate to prevent hazardous crush pressure."*
    - `#2 OPEN_ROUTE` on `z-corridor-1`: *"Provides immediate bypass relief into open sectors."*
    - `#3 DEPLOY_PERSONNEL` on `z-corridor-1`: *"Dispatches safety stewards for physical queue management."*
    - `#4 BROADCAST_ALERT`: Suggested public address announcement script.
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

### Beat 9: Citizen Mobile App & Closing (20–30s)
- **Action**: Show the native Android Citizen Mobile App running on phone or emulator connected to the live Render backend:
  - Tab 1: Live Venue Risk & Sector Badges
  - Tab 2: Active Incident Alerts Feed
  - Tab 3: Incident Report Dispatch
- **Narrative**:
  > "Simultaneously, citizens and on-ground stewards receive synchronized safety telemetry and can report incidents directly via our native Android mobile app. CrowdShield bridges the gap between high-level operations control and on-the-ground safety. Thank you."

---

## 3x Repeatability Test Summary
- Scenario switching between `s1` → `s2` → `s4` resets cleanly with zero residual state pollution.
- Live SSE stream updates seamlessly on both local and deployed Vercel/Render instances.
- Simulation runs statelessly without altering live playback metrics.
