# CrowdShield API Contract Draft (Phase 0)

Base path: `/api`  
Format: `application/json` except SSE stream endpoint.

All responses should include:
- `timestamp` (ISO-8601 UTC)
- `eventId` (string)

Quality fields used across payloads:
- `dataQuality`: `GOOD | DEGRADED | STALE | UNAVAILABLE`
- `confidence`: `0.0 .. 1.0`
- `reasons`: list of explainability/quality reasons

## 1) GET `/api/events/current`

Response `200`:
```json
{
  "timestamp": "2026-08-13T17:40:00Z",
  "eventId": "event-tech-nova-2026",
  "name": "TechNova Expo Day 2",
  "status": "LIVE",
  "venueId": "venue-main-ground",
  "phase": "MONITORING",
  "lastUpdateAt": "2026-08-13T17:39:58Z"
}
```

## 2) GET `/api/venue`

Response `200`:
```json
{
  "timestamp": "2026-08-13T17:40:00Z",
  "eventId": "event-tech-nova-2026",
  "venueId": "venue-main-ground",
  "name": "Main Ground",
  "zones": [
    { "zoneId": "z-entry-a", "name": "Entry A", "capacity": 450, "polygon": [[28.1,77.2],[28.1,77.21],[28.11,77.21]] }
  ],
  "edges": [
    { "from": "z-entry-a", "to": "z-corridor-1", "widthMeters": 4.0, "oneWay": false }
  ],
  "exits": ["z-exit-north", "z-exit-east"]
}
```

## 3) GET `/api/zones`

Response `200`:
```json
{
  "timestamp": "2026-08-13T17:40:00Z",
  "eventId": "event-tech-nova-2026",
  "zones": [
    {
      "zoneId": "z-entry-a",
      "densityPerSqM": 2.8,
      "flowInPerMin": 95,
      "flowOutPerMin": 70,
      "speedMpsP50": 0.95,
      "queueLength": 18,
      "occupancyPct": 78,
      "dataQuality": "GOOD",
      "confidence": 0.88,
      "reasons": ["optical_flow_consistent"]
    }
  ]
}
```

## 4) GET `/api/risk/current`

Response `200`:
```json
{
  "timestamp": "2026-08-13T17:40:00Z",
  "eventId": "event-tech-nova-2026",
  "overallRisk": {
    "score": 0.67,
    "level": "HIGH",
    "trend": "RISING",
    "forecast2m": 0.74,
    "forecast5m": 0.79,
    "confidence": 0.81,
    "reasons": ["high_density", "outflow_bottleneck", "speed_drop"]
  },
  "zoneRisks": [
    {
      "zoneId": "z-corridor-1",
      "score": 0.82,
      "level": "CRITICAL",
      "trend": "RISING",
      "holdUntil": "2026-08-13T17:41:00Z",
      "reasons": ["density_above_threshold", "counter_flow_detected"]
    }
  ]
}
```

## 5) GET `/api/alerts`

Query params:
- `active=true|false` (optional)
- `since=<ISO-8601>` (optional)

Response `200`:
```json
{
  "timestamp": "2026-08-13T17:40:00Z",
  "eventId": "event-tech-nova-2026",
  "alerts": [
    {
      "alertId": "al-1022",
      "zoneId": "z-corridor-1",
      "type": "CONGESTION_RISK",
      "severity": "HIGH",
      "status": "ACTIVE",
      "firstRaisedAt": "2026-08-13T17:38:10Z",
      "lastUpdatedAt": "2026-08-13T17:39:55Z",
      "dedupeKey": "z-corridor-1|CONGESTION_RISK|HIGH|2026-08-13T17:39Z",
      "message": "Reroute inflow from Entry A to Entry C."
    }
  ]
}
```

## 6) POST `/api/simulations`

Request:
```json
{
  "eventId": "event-tech-nova-2026",
  "scenarioName": "close-gate-b-open-corridor-c",
  "actions": [
    { "type": "CLOSE_GATE", "targetId": "gate-b", "atOffsetSec": 0 },
    { "type": "OPEN_ROUTE", "targetId": "corridor-c", "atOffsetSec": 20 }
  ],
  "horizonSec": 300
}
```

Response `200`:
```json
{
  "timestamp": "2026-08-13T17:40:00Z",
  "eventId": "event-tech-nova-2026",
  "simulationId": "sim-331",
  "baseline": { "peakRisk": 0.84, "timeToPeakSec": 180 },
  "projected": { "peakRisk": 0.66, "timeToPeakSec": 220 },
  "delta": { "peakRisk": -0.18, "estimatedClearanceSec": -140 },
  "recommendation": "Apply scenario within 60s for meaningful reduction."
}
```

Validation errors `400`:
```json
{
  "timestamp": "2026-08-13T17:40:00Z",
  "eventId": "event-tech-nova-2026",
  "error": "INVALID_SIMULATION_REQUEST",
  "details": ["actions[0].targetId does not exist"]
}
```

## 7) POST `/api/incidents`

Request:
```json
{
  "eventId": "event-tech-nova-2026",
  "reporterType": "CITIZEN",
  "location": { "lat": 28.1022, "lon": 77.2011, "zoneId": "z-corridor-1" },
  "category": "FALL_INJURY",
  "severity": "MEDIUM",
  "description": "Person slipped near barricade",
  "reportedAt": "2026-08-13T17:39:20Z"
}
```

Response `202`:
```json
{
  "timestamp": "2026-08-13T17:40:00Z",
  "eventId": "event-tech-nova-2026",
  "incidentId": "inc-881",
  "status": "RECEIVED",
  "triageQueuePosition": 3
}
```

Validation errors `400`:
```json
{
  "timestamp": "2026-08-13T17:40:00Z",
  "eventId": "event-tech-nova-2026",
  "error": "INVALID_INCIDENT_REPORT",
  "details": ["category is required", "location.zoneId unknown"]
}
```

## 8) GET `/api/routes/safest`

Query params:
- `fromZoneId` (required)
- `toExitId` (optional, default nearest safe exit)

Response `200`:
```json
{
  "timestamp": "2026-08-13T17:40:00Z",
  "eventId": "event-tech-nova-2026",
  "routeId": "rt-92",
  "fromZoneId": "z-corridor-1",
  "toExitId": "z-exit-east",
  "estimatedMinutes": 4.5,
  "riskWeightedScore": 0.22,
  "steps": [
    { "zoneId": "z-corridor-1", "instruction": "Proceed south for 80m" },
    { "zoneId": "z-open-yard", "instruction": "Turn east toward Exit East" }
  ],
  "validUntil": "2026-08-13T17:41:00Z"
}
```

Offline/degraded suggestion `206`:
```json
{
  "timestamp": "2026-08-13T17:40:00Z",
  "eventId": "event-tech-nova-2026",
  "routeId": "cached-rt-12",
  "source": "CACHE",
  "warning": "Live backend unavailable; route may be stale."
}
```

## 9) GET `/api/stream` (SSE)

Headers:
- `Content-Type: text/event-stream`

Event examples:
```text
event: zone_metrics
data: {"timestamp":"2026-08-13T17:40:01Z","eventId":"event-tech-nova-2026","zoneId":"z-entry-a","occupancyPct":79,"dataQuality":"GOOD"}

event: risk_update
data: {"timestamp":"2026-08-13T17:40:02Z","eventId":"event-tech-nova-2026","overallRisk":{"score":0.69,"level":"HIGH"}}

event: alert_raised
data: {"timestamp":"2026-08-13T17:40:04Z","eventId":"event-tech-nova-2026","alertId":"al-1023","zoneId":"z-corridor-1","severity":"CRITICAL"}
```

Keepalive:
- Send heartbeat every 10-15s to prevent stale UI connections.

## Cross-endpoint edge-case contract rules

- If no camera feed: set `dataQuality=UNAVAILABLE`, keep service alive, continue emitting status updates.
- If low FPS or missing windows: set `dataQuality=DEGRADED|STALE`, lower confidence, include reason codes.
- Use hysteresis (`holdUntil`) to reduce score flicker.
- Use dedupe keys to suppress repeated equivalent alerts.
