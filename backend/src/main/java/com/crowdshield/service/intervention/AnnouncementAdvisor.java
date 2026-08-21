package com.crowdshield.service.intervention;

import com.crowdshield.model.CurrentAnalyticsResponse;
import com.crowdshield.model.Recommendation;
import com.crowdshield.model.RecommendationActionType;
import com.crowdshield.model.RiskLevel;
import com.crowdshield.model.RiskResponse;
import com.crowdshield.model.ZoneAnalytics;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AnnouncementAdvisor {

    public List<Recommendation> evaluate(RiskResponse risk, CurrentAnalyticsResponse analytics) {
        List<Recommendation> recommendations = new ArrayList<>();
        if (risk == null || risk.overallRisk() == null) {
            return recommendations;
        }

        RiskLevel overallLevel = risk.overallRisk().level();
        boolean hasCriticalZone = analytics != null && analytics.zones() != null && analytics.zones().stream()
                .anyMatch(z -> z.bottleneck().severity() == RiskLevel.CRITICAL || z.counterflow().severity() == RiskLevel.CRITICAL);
        boolean hasCounterflow = analytics != null && analytics.counterflowZones() != null && !analytics.counterflowZones().isEmpty();

        if (overallLevel.ordinal() >= RiskLevel.HIGH.ordinal() || hasCriticalZone || hasCounterflow) {
            RiskLevel urgency = (overallLevel == RiskLevel.CRITICAL || hasCriticalZone) ? RiskLevel.CRITICAL : RiskLevel.HIGH;

            String tone;
            String suggestedMsg;
            if (hasCounterflow) {
                tone = "URGENT_CALMING";
                suggestedMsg = "Public Announcement: Please remain calm, maintain single-file movement, and follow safety stewards toward Exit East. Do not push.";
            } else if (overallLevel == RiskLevel.CRITICAL || hasCriticalZone) {
                tone = "URGENT_DIRECTIONAL";
                suggestedMsg = "Public Announcement: Central corridors are experiencing heavy crowd density. Please utilize open peripheral routes and follow directional signs.";
            } else {
                tone = "INFORMATIONAL_GUIDANCE";
                suggestedMsg = "Public Announcement: Steady crowd flow is currently observed. Please proceed smoothly toward open sectors.";
            }

            String reason = String.format(
                    "Overall event risk is %s (confidence %.2f) with active congestion triggers in transit zones.",
                    overallLevel, risk.overallRisk().confidence());
            String impact = "Provides synchronized, calm communication across PA audio and mobile feeds to prevent panic and guide egress.";

            recommendations.add(new Recommendation(
                    "rec-announcement-" + UUID.randomUUID().toString().substring(0, 8),
                    RecommendationActionType.BROADCAST_ALERT,
                    "venue-wide",
                    "BROADCAST",
                    "Recommend Public Safety Broadcast (" + tone + ")",
                    reason,
                    impact,
                    risk.overallRisk().confidence(),
                    urgency,
                    suggestedMsg,
                    0));
        }

        return recommendations;
    }
}
