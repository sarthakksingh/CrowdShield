package com.crowdshield.model;

import java.util.List;

public record RiskResponse(
        OverallRisk overallRisk,
        List<ZoneRisk> zoneRisks) {}
