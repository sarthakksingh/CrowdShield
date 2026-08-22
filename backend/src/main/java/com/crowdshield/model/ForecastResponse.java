package com.crowdshield.model;

import java.util.List;

public record ForecastResponse(
        String timestamp,
        String eventId,
        String scenarioId,
        int currentOffsetSec,
        List<Integer> horizonsSec,
        OverallForecast overallForecast,
        List<ZoneForecast> zoneForecasts,
        String disclaimer) {}
