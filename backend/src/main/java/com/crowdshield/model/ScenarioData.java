package com.crowdshield.model;

import java.util.List;

public record ScenarioData(
        String eventId,
        String name,
        String status,
        String venueId,
        String phase,
        String trend,
        double confidence,
        List<ZoneMetric> zoneMetrics) {}
