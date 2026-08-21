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
public class RouteAdvisor {

    public List<Recommendation> evaluate(RiskResponse risk, CurrentAnalyticsResponse analytics) {
        List<Recommendation> recommendations = new ArrayList<>();
        if (analytics == null || analytics.zones() == null) {
            return recommendations;
        }

        for (ZoneAnalytics zone : analytics.zones()) {
            // Counterflow Redirection
            if (zone.counterflow().isCounterflowDetected() || zone.movementInstability() >= 0.50) {
                RiskLevel urgency = zone.counterflow().severity().ordinal() >= RiskLevel.HIGH.ordinal()
                        ? zone.counterflow().severity()
                        : RiskLevel.HIGH;

                String reason = String.format(
                        "Opposing crowd movement detected in %s (opposing score %.2f, instability %.2f).",
                        zone.zoneId(), zone.counterflow().opposingMovementScore(), zone.movementInstability());
                String impact = "Enforces unidirectional laminar movement, preventing bidirectional collision and localized panic.";

                recommendations.add(new Recommendation(
                        "rec-route-redirect-" + UUID.randomUUID().toString().substring(0, 8),
                        RecommendationActionType.REDIRECT_INFLOW,
                        zone.zoneId(),
                        "ROUTE",
                        "Recommend Redirecting Flow to Secondary Pathway",
                        reason,
                        impact,
                        zone.confidence(),
                        urgency,
                        "Operations advisory: Guide incoming pedestrian stream away from " + zone.zoneId() + " using directional barriers.",
                        0));
            }

            // Route Opening for Congested Corridors
            if (zone.speedDropPct() >= 40.0 || (zone.bottleneck().isBottleneck() && zone.densityPerSqM() >= 2.8)) {
                RiskLevel urgency = zone.bottleneck().severity().ordinal() >= RiskLevel.HIGH.ordinal()
                        ? zone.bottleneck().severity()
                        : RiskLevel.MODERATE;

                String reason = String.format(
                        "Significant speed degradation of %.1f%% and high density (%.1f/m²) in %s.",
                        zone.speedDropPct(), zone.densityPerSqM(), zone.zoneId());
                String impact = "Provides immediate bypass relief, diverting transit volume away from the primary choke point.";

                recommendations.add(new Recommendation(
                        "rec-route-open-" + UUID.randomUUID().toString().substring(0, 8),
                        RecommendationActionType.OPEN_ROUTE,
                        zone.zoneId(),
                        "ROUTE",
                        "Recommend Opening Secondary Bypass Corridor",
                        reason,
                        impact,
                        zone.confidence(),
                        urgency,
                        "Operations advisory: Open auxiliary barrier/route connecting " + zone.zoneId() + " to open sectors.",
                        0));
            }
        }

        return recommendations;
    }
}
