package com.crowdshield.model;

import java.util.List;

public record ZoneMetric(
        String zoneId,
        double densityPressure,
        double inflowOutflowImbalance,
        double movementInstability,
        double opposingMovement,
        double bottleneckPressure,
        double routeAvailability,
        double densityPerSqM,
        int flowInPerMin,
        int flowOutPerMin,
        double speedMpsP50,
        int queueLength,
        int occupancyPct,
        String dataQuality,
        double confidence,
        List<String> reasons) {}
