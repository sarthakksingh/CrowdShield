package com.crowdshield.model;

import java.util.List;

public record ZoneAnalytics(
        String zoneId,
        double densityPerSqM,
        double densityPressure,
        TrendDirection densityTrend,
        double densityDelta,
        int flowInPerMin,
        TrendDirection inflowTrend,
        int flowOutPerMin,
        TrendDirection outflowTrend,
        int netPressure,
        double speedMpsP50,
        double speedDropPct,
        int queueLength,
        int queueGrowth,
        TrendDirection queueTrend,
        double movementInstability,
        RiskLevel instabilityLevel,
        BottleneckInfo bottleneck,
        CounterflowInfo counterflow,
        double confidence,
        String dataQuality,
        List<String> reasons) {}
