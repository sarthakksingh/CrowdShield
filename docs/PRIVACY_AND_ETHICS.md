# Privacy & Ethics Principles (Phase 0)

## Scope

CrowdShield is a **safety decision-support system**, not a person surveillance platform.

## Non-negotiable constraints

- No facial recognition.
- No identity tracking.
- No storage of real personal identities.
- Do not store raw CCTV by default.
- No unverifiable "AI predicts stampede" claims.

## Data minimization

- Prefer zone-level aggregate metrics over individual trajectories.
- Store only what is needed for short-horizon risk and response decisions.
- Keep retention short for operational telemetry.
- Redact free-text incident data where possible.

## Transparency and explainability

- Every risk output must include contributing factors and confidence.
- Every recommendation must include "why this action" and expected impact.
- Clearly label degraded/low-confidence states.

## Human-in-the-loop safety

- Recommendations are advisory; final actions remain with human operators.
- Show uncertainty explicitly to prevent over-trust.
- Require explicit operator acknowledgment for high-impact interventions.

## Bias and harm reduction

- Use infrastructure and crowd-flow signals, not demographic attributes.
- Test scenarios for false positives/false negatives and document trade-offs.
- Include fail-safe messaging to avoid panic-inducing alerts.

## Mobile user safety messaging

- Use calm, actionable language.
- Avoid absolute guarantees ("safe", "danger free").
- Provide alternate routes and emergency instructions with timestamps.

## Governance for this prototype

- Maintain an assumptions log in docs.
- Record known limitations in every demo readout.
- Keep a visible "prototype mode" banner in dashboard/mobile.
