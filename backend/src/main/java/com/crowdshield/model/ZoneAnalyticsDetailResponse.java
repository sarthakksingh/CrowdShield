package com.crowdshield.model;

import java.util.List;

public record ZoneAnalyticsDetailResponse(
        String timestamp,
        String eventId,
        String scenarioId,
        int currentOffsetSec,
        ZoneAnalytics analytics,
        List<ZoneAnalyticsTimelinePoint> timeline) {}
