package com.crowdshield.model;

public record ZoneAnalyticsTimelinePoint(
        int offsetSec,
        double densityPerSqM,
        int flowInPerMin,
        int flowOutPerMin,
        int netPressure,
        double speedMpsP50,
        double speedDropPct,
        int queueLength,
        double movementInstability,
        double bottleneckScore,
        boolean isBottleneck,
        boolean isCounterflowDetected) {}
