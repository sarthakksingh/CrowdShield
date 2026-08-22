package com.crowdshield.model;

import java.util.List;
import java.util.Map;

public record OverallForecast(
        double currentScore,
        RiskLevel currentLevel,
        Map<Integer, HorizonRiskForecast> horizons,
        String trend,
        double confidence,
        int timeToCriticalSec,
        String horizonEstimate,
        List<String> reasons) {}
