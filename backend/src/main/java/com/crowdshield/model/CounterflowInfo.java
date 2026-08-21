package com.crowdshield.model;

import java.util.List;

public record CounterflowInfo(
        String zoneId,
        boolean isCounterflowDetected,
        RiskLevel severity,
        double opposingMovementScore,
        List<String> reasons) {}
