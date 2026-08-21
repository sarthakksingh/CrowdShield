package com.crowdshield.model;

import java.util.List;

public record RiskResponse(
        OverallRisk overallRisk,
        List<ZoneRisk> zoneRisks,
        String disclaimer) {

    public RiskResponse(OverallRisk overallRisk, List<ZoneRisk> zoneRisks) {
        this(overallRisk, zoneRisks, overallRisk != null ? overallRisk.disclaimer() : "");
    }
}
