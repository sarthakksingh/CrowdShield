package com.crowdshield.model;

import java.time.Instant;
import java.util.List;

public record PlaybackState(
        String scenarioId,
        String status,
        int currentOffsetSec,
        int durationSec,
        double speed,
        Instant startedAt,
        Instant lastUpdateAt,
        Frame currentFrame,
        List<ZoneMetric> currentZoneMetrics) {}
