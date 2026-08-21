package com.crowdshield.model;

import java.util.List;
import java.util.Map;

public record RecommendationsResponse(
        String timestamp,
        String eventId,
        String scenarioId,
        int currentOffsetSec,
        RiskLevel overallRiskLevel,
        List<Recommendation> recommendations,
        String advisoryNotice,
        Map<String, Object> summary) {}
