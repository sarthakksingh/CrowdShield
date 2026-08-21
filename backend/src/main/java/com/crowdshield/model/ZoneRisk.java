package com.crowdshield.model;

import java.util.List;
import java.util.Map;

public record ZoneRisk(
        String zoneId,
        double score,
        RiskLevel level,
        String trend,
        double confidence,
        List<String> reasons,
        Map<String, Double> factorContributions,
        String holdUntil) {}
