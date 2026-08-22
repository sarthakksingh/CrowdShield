package com.crowdshield.service;

import com.crowdshield.config.CrowdShieldProperties;
import com.crowdshield.model.OverallRisk;
import com.crowdshield.model.PlaybackState;
import com.crowdshield.model.RiskConfigDto;
import com.crowdshield.model.RiskLevel;
import com.crowdshield.model.RiskResponse;
import com.crowdshield.model.ZoneMetric;
import com.crowdshield.model.ZoneRisk;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class RiskScoringService {
    private static final String KEY_OVERALL = "__overall__";

    private final ScenarioPlaybackService playbackService;
    private final RiskStateService riskStateService;
    private final AlertService alertService;
    private final Map<String, Double> weights = new ConcurrentHashMap<>();

    private volatile double moderateThreshold;
    private volatile double highThreshold;
    private volatile double criticalThreshold;
    private volatile String disclaimer;

    private String lastScenarioId;
    private int lastOffsetSec = -1;

    public RiskScoringService(
            ScenarioPlaybackService playbackService,
            RiskStateService riskStateService,
            AlertService alertService,
            CrowdShieldProperties properties) {
        this.playbackService = playbackService;
        this.riskStateService = riskStateService;
        this.alertService = alertService;

        // Initialize weights
        Map<String, Double> propWeights = properties.getRisk().getWeights();
        if (propWeights != null && !propWeights.isEmpty()) {
            propWeights.forEach((k, v) -> weights.put(normalizeKey(k), v));
        } else {
            weights.put("density_pressure", 0.24);
            weights.put("inflow_outflow_imbalance", 0.20);
            weights.put("movement_instability", 0.16);
            weights.put("opposing_movement", 0.14);
            weights.put("bottleneck_pressure", 0.18);
            weights.put("route_availability", 0.08);
        }

        // Initialize thresholds
        CrowdShieldProperties.Risk.Thresholds th = properties.getRisk().getThresholds();
        this.moderateThreshold = th != null ? th.getModerate() : 0.35;
        this.highThreshold = th != null ? th.getHigh() : 0.55;
        this.criticalThreshold = th != null ? th.getCritical() : 0.75;

        // Initialize disclaimer
        this.disclaimer = properties.getRisk().getDisclaimer();
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

            Map<String, String> factorDescriptions = buildFactorDescriptions(scored.contributions, scored.score);
            String horizon = estimateZoneHorizon(scored.score, snapshot.level(), snapshot.trend());

            zoneRisks.add(new ZoneRisk(
                    metric.zoneId(),
                    round(scored.score),
                    snapshot.level(),
                    snapshot.trend(),
                    metric.confidence(),
                    scored.reasons,
                    scored.contributions,
                    factorDescriptions,
                    horizon,
                    DateTimeFormatter.ISO_INSTANT.format(snapshot.holdUntil()),
                    disclaimer));
        }

        double averageScore = zoneRisks.stream().mapToDouble(ZoneRisk::score).average().orElse(0.0);
        double peakScore = zoneRisks.stream().mapToDouble(ZoneRisk::score).max().orElse(0.0);
        double overallScore = (peakScore * 0.65) + (averageScore * 0.35);
        RiskLevel overallComputed = levelFor(overallScore);
        RiskStateService.RiskSnapshot overallSnapshot =
                riskStateService.apply(KEY_OVERALL, overallComputed, overallScore, now);

        Map<String, Double> overallContributions = aggregateContributions(zoneRisks);
        Map<String, String> overallFactorDescriptions = buildFactorDescriptions(overallContributions, overallScore);
        String overallHorizon = estimateOverallHorizon(overallScore, overallSnapshot.level(), overallSnapshot.trend());

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
                overallFactorDescriptions,
                overallHorizon,
                DateTimeFormatter.ISO_INSTANT.format(overallSnapshot.holdUntil()),
                disclaimer);

        return new RiskResponse(overallRisk, zoneRisks, disclaimer);
    }

    public RiskResponse evaluateMetricsStateless(List<ZoneMetric> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            OverallRisk emptyOverall = new OverallRisk(0.0, RiskLevel.LOW, "STABLE", 1.0, List.of("nominal"), Map.of(), Map.of(), "stable", "", disclaimer);
            return new RiskResponse(emptyOverall, List.of(), disclaimer);
        }

        List<ZoneRisk> zoneRisks = new ArrayList<>();
        for (ZoneMetric metric : metrics) {
            ScoredRisk scored = score(metric);
            RiskLevel computedLevel = levelFor(scored.score);
            Map<String, String> factorDescriptions = buildFactorDescriptions(scored.contributions, scored.score);
            String horizon = estimateZoneHorizon(scored.score, computedLevel, "STABLE");

            zoneRisks.add(new ZoneRisk(
                    metric.zoneId(),
                    round(scored.score),
                    computedLevel,
                    "STABLE",
                    metric.confidence(),
                    scored.reasons,
                    scored.contributions,
                    factorDescriptions,
                    horizon,
                    "",
                    disclaimer));
        }

        double averageScore = zoneRisks.stream().mapToDouble(ZoneRisk::score).average().orElse(0.0);
        double peakScore = zoneRisks.stream().mapToDouble(ZoneRisk::score).max().orElse(0.0);
        double overallScore = (peakScore * 0.65) + (averageScore * 0.35);
        RiskLevel overallComputed = levelFor(overallScore);

        Map<String, Double> overallContributions = aggregateContributions(zoneRisks);
        Map<String, String> overallFactorDescriptions = buildFactorDescriptions(overallContributions, overallScore);
        String overallHorizon = estimateOverallHorizon(overallScore, overallComputed, "STABLE");

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
                overallComputed,
                "STABLE",
                round(overallConfidence),
                overallReasons,
                overallContributions,
                overallFactorDescriptions,
                overallHorizon,
                "",
                disclaimer);

        return new RiskResponse(overallRisk, zoneRisks, disclaimer);
    }


    public synchronized RiskConfigDto getRiskConfig() {
        Map<String, Double> thresholdsMap = new LinkedHashMap<>();
        thresholdsMap.put("moderate", moderateThreshold);
        thresholdsMap.put("high", highThreshold);
        thresholdsMap.put("critical", criticalThreshold);

        return new RiskConfigDto(
                thresholdsMap,
                new LinkedHashMap<>(weights),
                riskStateService.getHoldSeconds(),
                alertService.getRequiredConsecutiveFrames(),
                disclaimer);
    }

    public synchronized RiskConfigDto updateRiskConfig(RiskConfigDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Risk configuration body cannot be null");
        }

        // Validate thresholds
        if (dto.thresholds() != null) {
            Double mod = dto.thresholds().get("moderate");
            Double hi = dto.thresholds().get("high");
            Double crit = dto.thresholds().get("critical");

            if (mod != null && hi != null && crit != null) {
                if (mod <= 0.0 || mod >= hi || hi >= crit || crit > 1.0) {
                    throw new IllegalArgumentException("Thresholds must satisfy 0.0 < moderate < high < critical <= 1.0");
                }
                this.moderateThreshold = mod;
                this.highThreshold = hi;
                this.criticalThreshold = crit;
            }
        }

        // Validate weights
        if (dto.weights() != null && !dto.weights().isEmpty()) {
            for (Map.Entry<String, Double> entry : dto.weights().entrySet()) {
                if (entry.getValue() == null || entry.getValue() < 0.0) {
                    throw new IllegalArgumentException("Weight for " + entry.getKey() + " must be non-negative");
                }
            }
            weights.clear();
            dto.weights().forEach((k, v) -> weights.put(normalizeKey(k), v));
        }

        // Update persistence & hysteresis
        if (dto.hysteresisHoldSeconds() > 0) {
            riskStateService.setHoldSeconds(dto.hysteresisHoldSeconds());
        }
        if (dto.persistenceRequiredFrames() > 0) {
            alertService.setRequiredConsecutiveFrames(dto.persistenceRequiredFrames());
        }
        if (dto.disclaimer() != null && !dto.disclaimer().isBlank()) {
            this.disclaimer = dto.disclaimer();
        }

        return getRiskConfig();
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

    public ScoredRisk score(ZoneMetric metric) {
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

    private Map<String, String> buildFactorDescriptions(Map<String, Double> contributions, double score) {
        Map<String, String> descriptions = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : contributions.entrySet()) {
            String key = entry.getKey();
            double val = entry.getValue();
            double pct = score > 0 ? round((val / score) * 100.0) : 0.0;
            String label = key.replace('_', ' ');
            descriptions.put(key, String.format("%s contributed %.1f%% to risk (weighted: %.3f)",
                    capitalize(label), pct, val));
        }
        return descriptions;
    }

    private String estimateZoneHorizon(double score, RiskLevel level, String trend) {
        if (level == RiskLevel.CRITICAL) {
            return "immediate (< 1 min)";
        }
        if (level == RiskLevel.HIGH) {
            if ("RISING".equalsIgnoreCase(trend)) {
                return "1-2 min";
            } else if ("FALLING".equalsIgnoreCase(trend)) {
                return "2-4 min (recovering)";
            } else {
                return "2-4 min";
            }
        }
        if (level == RiskLevel.MODERATE) {
            if ("RISING".equalsIgnoreCase(trend)) {
                return "3-5 min";
            } else {
                return "5-10 min";
            }
        }
        if ("RISING".equalsIgnoreCase(trend)) {
            return "8-12 min";
        }
        return "> 15 min / stable";
    }

    private String estimateOverallHorizon(double score, RiskLevel level, String trend) {
        if (level == RiskLevel.CRITICAL) {
            return "immediate (< 1 min)";
        }
        if (level == RiskLevel.HIGH) {
            return "RISING".equalsIgnoreCase(trend) ? "1-3 min" : "2-5 min";
        }
        if (level == RiskLevel.MODERATE) {
            return "RISING".equalsIgnoreCase(trend) ? "4-6 min" : "6-12 min";
        }
        return "> 15 min / stable";
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
        String normalized = normalizeKey(key);
        return weights.getOrDefault(normalized, 0.1);
    }

    private String normalizeKey(String key) {
        return key.toLowerCase().replace('-', '_');
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    public RiskLevel levelFor(double score) {
        if (score < moderateThreshold) {
            return RiskLevel.LOW;
        }
        if (score < highThreshold) {
            return RiskLevel.MODERATE;
        }
        if (score < criticalThreshold) {
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

    public record ScoredRisk(double score, Map<String, Double> contributions, List<String> reasons) {}
}
