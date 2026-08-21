package com.crowdshield.model;

import java.util.List;
import java.util.Map;

public record BottleneckInfo(
        String zoneId,
        boolean isBottleneck,
        RiskLevel severity,
        double bottleneckScore,
        List<String> reasons,
        Map<String, Double> contributingFactors) {}
