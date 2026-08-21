package com.crowdshield.service.intervention;

import com.crowdshield.model.CurrentAnalyticsResponse;
import com.crowdshield.model.Recommendation;
import com.crowdshield.model.RecommendationActionType;
import com.crowdshield.model.RiskLevel;
import com.crowdshield.model.RiskResponse;
import com.crowdshield.model.ZoneAnalytics;
import com.crowdshield.model.ZoneRisk;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class GateAdvisor {

    public List<Recommendation> evaluate(RiskResponse risk, CurrentAnalyticsResponse analytics) {
        List<Recommendation> recommendations = new ArrayList<>();
        if (analytics == null || analytics.zones() == null) {
            return recommendations;
        }

        Map<String, ZoneRisk> riskMap = risk != null && risk.zoneRisks() != null
                ? risk.zoneRisks().stream().collect(java.util.stream.Collectors.toMap(ZoneRisk::zoneId, z -> z))
                : Map.of();

        for (ZoneAnalytics zone : analytics.zones()) {
            ZoneRisk zoneRisk = riskMap.get(zone.zoneId());
            RiskLevel level = zoneRisk != null ? zoneRisk.level() : zone.bottleneck().severity();

            // Gate Restriction Advisor
            if (zone.bottleneck().isBottleneck() || zone.netPressure() >= 30 || zone.densityPerSqM() >= 3.0) {
                String targetGate = zone.zoneId().contains("entry") ? zone.zoneId() : "z-entry-a";
                RiskLevel urgency = level.ordinal() >= RiskLevel.HIGH.ordinal() ? level : RiskLevel.HIGH;

                String reason = String.format(
                        "Elevated crowd accumulation in %s (density %.1f/m², net pressure +%d/min, queue %d).",
                        zone.zoneId(), zone.densityPerSqM(), zone.netPressure(), zone.queueLength());
                String impact = "Meters incoming crowd rate to prevent hazardous crush pressure and allow downstream corridors to clear.";

                recommendations.add(new Recommendation(
                        "rec-gate-" + UUID.randomUUID().toString().substring(0, 8),
                        RecommendationActionType.RESTRICT_GATE,
                        targetGate,
                        "GATE",
                        "Recommend Restricting Entry Inflow",
                        reason,
                        impact,
                        zone.confidence(),
                        urgency,
                        "Operations advisory: Meter or pause turnstiles at " + targetGate + " for 3-5 minutes.",
                        0));
            }

            // Exit Opening Advisor
            if (zone.zoneId().contains("exit") || zone.zoneId().contains("open-yard")) {
                boolean upstreamCongested = analytics.zones().stream()
                        .anyMatch(z -> z.bottleneck().isBottleneck() || z.densityPerSqM() >= 2.5);
                if (upstreamCongested) {
                    RiskLevel urgency = level.ordinal() >= RiskLevel.HIGH.ordinal() ? RiskLevel.HIGH : RiskLevel.MODERATE;
                    String reason = "Upstream congestion detected; designated egress channels are required to accelerate dispersal.";
                    String impact = "Increases venue discharge throughput and reduces overall dwell time in dense sectors.";

                    recommendations.add(new Recommendation(
                            "rec-exit-" + UUID.randomUUID().toString().substring(0, 8),
                            RecommendationActionType.OPEN_EXIT,
                            zone.zoneId(),
                            "EXIT",
                            "Recommend Opening Auxiliary Exit Gates",
                            reason,
                            impact,
                            zone.confidence(),
                            urgency,
                            "Operations advisory: Fully open auxiliary gates at " + zone.zoneId() + " to expedite outflow.",
                            0));
                }
            }
        }

        return recommendations;
    }
}
