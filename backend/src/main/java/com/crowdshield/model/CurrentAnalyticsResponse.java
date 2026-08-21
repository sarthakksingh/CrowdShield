package com.crowdshield.model;

import java.util.List;
import java.util.Map;

public record CurrentAnalyticsResponse(
        String timestamp,
        String eventId,
        String scenarioId,
        int currentOffsetSec,
        List<BottleneckInfo> bottleneckZones,
        List<CounterflowInfo> counterflowZones,
        List<ZoneAnalytics> zones,
        Map<String, Object> summary) {}
