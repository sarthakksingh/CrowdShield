package com.crowdshield.model;

import java.util.List;
import java.util.Map;

public record ZoneForecast(
        String zoneId,
        double currentScore,
        RiskLevel currentLevel,
        Map<Integer, HorizonRiskForecast> horizons,
        String trend,
        String method,
        boolean methodsAgree,
        double confidence,
        String horizonEstimate,
        List<String> reasons) {}
