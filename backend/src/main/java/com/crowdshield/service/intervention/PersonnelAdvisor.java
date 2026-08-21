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
public class PersonnelAdvisor {

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

            boolean needsStewards = zone.queueLength() >= 15
                    || zone.movementInstability() >= 0.50
                    || zone.bottleneck().isBottleneck()
                    || level.ordinal() >= RiskLevel.HIGH.ordinal();

            if (needsStewards) {
                RiskLevel urgency = level.ordinal() >= RiskLevel.HIGH.ordinal() ? level : RiskLevel.HIGH;
                String reason = String.format(
                        "Elevated risk indicators in %s (queue %d persons, instability %.2f, bottleneck severity: %s).",
                        zone.zoneId(), zone.queueLength(), zone.movementInstability(), zone.bottleneck().severity());
                String impact = "Deploys on-the-ground marshals to guide crowd flow, enforce safe spacing, and assist distress cases.";

                recommendations.add(new Recommendation(
                        "rec-personnel-" + UUID.randomUUID().toString().substring(0, 8),
                        RecommendationActionType.DEPLOY_PERSONNEL,
                        zone.zoneId(),
                        "ZONE",
                        "Recommend Deploying Crowd Safety Stewards",
                        reason,
                        impact,
                        zone.confidence(),
                        urgency,
                        "Operations advisory: Dispatch response team of 4-6 safety marshals to " + zone.zoneId() + ".",
                        0));
            }
        }

        return recommendations;
    }
}
