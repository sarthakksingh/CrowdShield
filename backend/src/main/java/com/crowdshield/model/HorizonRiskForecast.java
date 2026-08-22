package com.crowdshield.model;

public record HorizonRiskForecast(
        int horizonSec,
        double score,
        RiskLevel level,
        double deltaFromCurrent) {}
