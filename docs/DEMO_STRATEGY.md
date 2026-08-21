# Demo Data & Reliability Strategy (Phase 0)

## Goal

Make the demo deterministic and reliable on an Intel Core i7 CPU, while still showing the full CrowdShield decision loop.

## Deterministic demo-data approach

Use scripted scenarios in `demo-data/`:

```text
demo-data/
  venue/
    venue_graph.json
    zones.geojson
  scenarios/
    s1_normal_flow.json
    s2_entry_bottleneck.json
    s3_counterflow_panic.json
    s4_post_intervention_recovery.json
  playback/
    timeline.csv
    metric_frames/*.json
```

Each scenario defines:
- Time-indexed zone metrics (density, inflow/outflow, speed, queue)
- Trigger events (gate closure, incident report)
- Expected risk trajectory and expected recommended actions

## Video strategy

- Primary: prerecorded clips mapped to scenario timelines.
- Optional: live camera input only as an enhancement, never a dependency.
- If video processing fails, switch to metric-frame replay mode automatically.

## Explainability-first risk model

Use a transparent weighted model for demo:
- `risk = w1*density + w2*flow_imbalance + w3*speed_drop + w4*queue_pressure`
- Output factor contributions in every risk response.

This avoids unverifiable "black-box AI" claims.

## Reliability controls

- Fixed random seeds for any stochastic step.
- Pinned dependencies and versioned scenario files.
- Golden output snapshots per scenario for quick pre-demo checks.
- Graceful degradation flags (`GOOD/DEGRADED/STALE/UNAVAILABLE`) everywhere.
- Alert deduplication and hysteresis to avoid noisy/flickering UX.

## Demo runbook (operator flow)

1. Load `s2_entry_bottleneck`.
2. Show rising risk in corridor and recommendation to reroute.
3. Run simulation (`close gate B`, `open corridor C`) and show projected drop.
4. Apply intervention state.
5. Replay `s4_post_intervention_recovery` and show verification (risk downtrend).
6. Trigger one incident report from mobile and show backend triage + route advisory update.

## Success criteria for demo reliability

- Runs end-to-end without internet dependency (external LLM/API optional only).
- Can replay identical outcome at least three times in a row.
- Continues functioning when camera feed is unavailable (degraded mode).
