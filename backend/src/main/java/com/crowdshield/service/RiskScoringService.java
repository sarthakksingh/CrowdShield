package com.crowdshield.service;

import com.crowdshield.config.CrowdShieldProperties;
import com.crowdshield.model.OverallRisk;
import com.crowdshield.model.PlaybackState;
import com.crowdshield.model.RiskLevel;
import com.crowdshield.model.RiskResponse;
import com.crowdshield.model.ZoneMetric;
import com.crowdshield.model.ZoneRisk;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class RiskScoringService {
    private static final String KEY_OVERALL = "__overall__";

    private final ScenarioPlaybackService playbackService;
    private final RiskStateService riskStateService;
    private final Map<String, Double> weights;
    private String lastScenarioId;
    private int lastOffsetSec = -1;

    public RiskScoringService(
            ScenarioPlaybackService playbackService,
            RiskStateService riskStateService,
            CrowdShieldProperties properties) {
        this.playbackService = playbackService;
        this.riskStateService = riskStateService;
        this.weights = properties.getRisk().getWeights();
    }

    public synchronized RiskResponse calculateCurrentRisk() {
        PlaybackState playbackState = playbackService.getState();
        resetTemporalStateIfPlaybackMovedBack(playbackState);
        List<ZoneMetric> metrics = playbackState.currentZoneMetrics();
        Instant now = Instant.now();

        List<ZoneRisk> zoneRisks = new ArrayList<>();
        for (ZoneMetric metric : metrics) {
            ScoredRisk scored = score(metric);
            RiskLevel computedLevel = levelFor(scored.score);
            RiskStateService.RiskSnapshot snapshot =
                    riskStateService.apply(metric.zoneId(), computedLevel, scored.score, now);

            zoneRisks.add(new ZoneRisk(
                    metric.zoneId(),
                    round(scored.score),
                    snapshot.level(),
                    snapshot.trend(),
                    metric.confidence(),
                    scored.reasons,
                    scored.contributions,
                    DateTimeFormatter.ISO_INSTANT.format(snapshot.holdUntil())));
        }

        double averageScore = zoneRisks.stream().mapToDouble(ZoneRisk::score).average().orElse(0.0);
        double peakScore = zoneRisks.stream().mapToDouble(ZoneRisk::score).max().orElse(0.0);
        double overallScore = (peakScore * 0.65) + (averageScore * 0.35);
        RiskLevel overallComputed = levelFor(overallScore);
        RiskStateService.RiskSnapshot overallSnapshot =
                riskStateService.apply(KEY_OVERALL, overallComputed, overallScore, now);

        Map<String, Double> overallContributions = aggregateContributions(zoneRisks);
        List<String> overallReasons = zoneRisks.stream()
                .filter(z -> z.level().ordinal() >= RiskLevel.HIGH.ordinal())
                .map(z -> "zone_" + z.zoneId() + "_elevated_risk")
                .collect(Collectors.toList());
        if (overallReasons.isEmpty()) {
            overallReasons = List.of("risk_within_prototype_threshold");
        }

        double overallConfidence = metrics.stream()
                .mapToDouble(ZoneMetric::confidence)
                .average()
                .orElse(0.86);

        OverallRisk overallRisk = new OverallRisk(
                round(overallScore),
                overallSnapshot.level(),
                overallSnapshot.trend(),
                round(overallConfidence),
                overallReasons,
                overallContributions,
                DateTimeFormatter.ISO_INSTANT.format(overallSnapshot.holdUntil()));

        return new RiskResponse(overallRisk, zoneRisks);
    }

    private void resetTemporalStateIfPlaybackMovedBack(PlaybackState playbackState) {
        boolean scenarioChanged = lastScenarioId != null && !lastScenarioId.equals(playbackState.scenarioId());
        boolean movedBackward = lastOffsetSec >= 0 && playbackState.currentOffsetSec() < lastOffsetSec;
        if (scenarioChanged || movedBackward) {
            riskStateService.clear();
        }
        lastScenarioId = playbackState.scenarioId();
        lastOffsetSec = playbackState.currentOffsetSec();
    }

    private ScoredRisk score(ZoneMetric metric) {
        Map<String, Double> rawFactors = new LinkedHashMap<>();
        rawFactors.put("density_pressure", metric.densityPressure());
        rawFactors.put("inflow_outflow_imbalance", metric.inflowOutflowImbalance());
        rawFactors.put("movement_instability", metric.movementInstability());
        rawFactors.put("opposing_movement", metric.opposingMovement());
        rawFactors.put("bottleneck_pressure", metric.bottleneckPressure());
        rawFactors.put("route_availability", 1.0 - metric.routeAvailability());

        double sumWeights = 0.0;
        double weightedSum = 0.0;
        Map<String, Double> contributions = new LinkedHashMap<>();
        List<String> reasons = new ArrayList<>();

        for (Map.Entry<String, Double> entry : rawFactors.entrySet()) {
            double weight = resolveWeight(entry.getKey());
            sumWeights += weight;
            double weighted = weight * clamp(entry.getValue());
            weightedSum += weighted;
            contributions.put(entry.getKey(), round(weighted));
            if (weighted >= 0.12) {
                reasons.add(entry.getKey() + "_elevated");
            }
        }

        if (reasons.isEmpty()) {
            reasons.add("no_elevated_factor_detected");
        }

        double score = sumWeights == 0.0 ? 0.0 : (weightedSum / sumWeights);
        return new ScoredRisk(round(score), contributions, reasons);
    }

    private Map<String, Double> aggregateContributions(List<ZoneRisk> zoneRisks) {
        Map<String, Double> sums = new LinkedHashMap<>();
        for (ZoneRisk zoneRisk : zoneRisks) {
            zoneRisk.factorContributions().forEach((key, value) -> sums.merge(key, value, Double::sum));
        }
        int count = Math.max(1, zoneRisks.size());
        sums.replaceAll((k, v) -> round(v / count));
        return sums;
    }

    private double resolveWeight(String key) {
        String propertyKey = key.replace('_', '-');
        return weights.getOrDefault(propertyKey, 0.1);
    }

    private RiskLevel levelFor(double score) {
        if (score < 0.35) {
            return RiskLevel.LOW;
        }
        if (score < 0.55) {
            return RiskLevel.MODERATE;
        }
        if (score < 0.75) {
            return RiskLevel.HIGH;
        }
        return RiskLevel.CRITICAL;
    }

    private double clamp(double v) {
        if (v < 0.0) {
            return 0.0;
        }
        if (v > 1.0) {
            return 1.0;
        }
        return v;
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private record ScoredRisk(double score, Map<String, Double> contributions, List<String> reasons) {}
}
