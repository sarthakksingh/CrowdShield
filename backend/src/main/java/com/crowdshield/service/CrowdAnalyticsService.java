package com.crowdshield.service;

import com.crowdshield.model.BottleneckInfo;
import com.crowdshield.model.CounterflowInfo;
import com.crowdshield.model.CurrentAnalyticsResponse;
import com.crowdshield.model.Frame;
import com.crowdshield.model.PlaybackState;
import com.crowdshield.model.RiskLevel;
import com.crowdshield.model.Scenario;
import com.crowdshield.model.TrendDirection;
import com.crowdshield.model.ZoneAnalytics;
import com.crowdshield.model.ZoneAnalyticsDetailResponse;
import com.crowdshield.model.ZoneAnalyticsTimelinePoint;
import com.crowdshield.model.ZoneMetric;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class CrowdAnalyticsService {
    private static final double FREE_FLOW_BASELINE_SPEED = 1.5; // m/s nominal standard free-flow speed

    private final ScenarioPlaybackService playbackService;
    private final EventStateService eventStateService;

    public CrowdAnalyticsService(
            ScenarioPlaybackService playbackService,
            EventStateService eventStateService) {
        this.playbackService = playbackService;
        this.eventStateService = eventStateService;
    }

    public CurrentAnalyticsResponse getCurrentAnalytics() {
        PlaybackState state = playbackService.getState();
        String eventId = eventStateService.getEventId();
        Frame currentFrame = state.currentFrame();
        Frame previousFrame = playbackService.getPreviousFrame();

        List<ZoneMetric> currentMetrics = currentFrame != null ? currentFrame.zoneMetrics() : Collections.emptyList();
        List<ZoneMetric> prevMetrics = previousFrame != null ? previousFrame.zoneMetrics() : Collections.emptyList();

        Map<String, ZoneMetric> prevMetricMap = new LinkedHashMap<>();
        for (ZoneMetric m : prevMetrics) {
            prevMetricMap.put(m.zoneId(), m);
        }

        List<ZoneAnalytics> zoneAnalyticsList = new ArrayList<>();
        List<BottleneckInfo> bottleneckZones = new ArrayList<>();
        List<CounterflowInfo> counterflowZones = new ArrayList<>();

        for (ZoneMetric metric : currentMetrics) {
            ZoneMetric prev = prevMetricMap.get(metric.zoneId());
            ZoneAnalytics za = computeZoneAnalytics(metric, prev);
            zoneAnalyticsList.add(za);

            if (za.bottleneck().isBottleneck()) {
                bottleneckZones.add(za.bottleneck());
            }
            if (za.counterflow().isCounterflowDetected()) {
                counterflowZones.add(za.counterflow());
            }
        }

        // Summary metrics
        Map<String, Object> summary = new LinkedHashMap<>();
        Optional<ZoneAnalytics> maxBottleneck = zoneAnalyticsList.stream()
                .max(Comparator.comparingDouble(z -> z.bottleneck().bottleneckScore()));
        Optional<ZoneAnalytics> maxNetPressure = zoneAnalyticsList.stream()
                .max(Comparator.comparingInt(ZoneAnalytics::netPressure));
        Optional<ZoneAnalytics> maxInstability = zoneAnalyticsList.stream()
                .max(Comparator.comparingDouble(ZoneAnalytics::movementInstability));

        double avgInstability = zoneAnalyticsList.stream()
                .mapToDouble(ZoneAnalytics::movementInstability)
                .average()
                .orElse(0.0);

        summary.put("maxBottleneckZone", maxBottleneck.map(ZoneAnalytics::zoneId).orElse(null));
        summary.put("maxBottleneckScore", maxBottleneck.map(z -> z.bottleneck().bottleneckScore()).orElse(0.0));
        summary.put("maxNetPressureZone", maxNetPressure.map(ZoneAnalytics::zoneId).orElse(null));
        summary.put("maxNetPressure", maxNetPressure.map(ZoneAnalytics::netPressure).orElse(0));
        summary.put("highestInstabilityZone", maxInstability.map(ZoneAnalytics::zoneId).orElse(null));
        summary.put("averageMovementInstability", round(avgInstability));
        summary.put("totalBottlenecksDetected", bottleneckZones.size());
        summary.put("totalCounterflowsDetected", counterflowZones.size());

        return new CurrentAnalyticsResponse(
                Instant.now().toString(),
                eventId,
                state.scenarioId(),
                state.currentOffsetSec(),
                bottleneckZones,
                counterflowZones,
                zoneAnalyticsList,
                summary);
    }

    public ZoneAnalyticsDetailResponse getZoneAnalytics(String zoneId) {
        if (zoneId == null || zoneId.trim().isEmpty()) {
            throw new IllegalArgumentException("Zone ID cannot be blank");
        }

        PlaybackState state = playbackService.getState();
        String eventId = eventStateService.getEventId();
        Frame currentFrame = state.currentFrame();
        Frame previousFrame = playbackService.getPreviousFrame();

        List<ZoneMetric> currentMetrics = currentFrame != null ? currentFrame.zoneMetrics() : Collections.emptyList();
        ZoneMetric currentZoneMetric = currentMetrics.stream()
                .filter(m -> m.zoneId().equalsIgnoreCase(zoneId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Zone not found: " + zoneId));

        ZoneMetric prevZoneMetric = null;
        if (previousFrame != null) {
            prevZoneMetric = previousFrame.zoneMetrics().stream()
                    .filter(m -> m.zoneId().equalsIgnoreCase(zoneId))
                    .findFirst()
                    .orElse(null);
        }

        ZoneAnalytics analytics = computeZoneAnalytics(currentZoneMetric, prevZoneMetric);

        // Build timeline history
        List<Frame> framesUpToOffset = playbackService.getFramesUpToCurrentOffset();
        List<ZoneAnalyticsTimelinePoint> timeline = new ArrayList<>();

        for (Frame f : framesUpToOffset) {
            f.zoneMetrics().stream()
                    .filter(m -> m.zoneId().equalsIgnoreCase(zoneId))
                    .findFirst()
                    .ifPresent(m -> {
                        int netPres = m.flowInPerMin() - m.flowOutPerMin();
                        double speedDrop = computeSpeedDropPct(m.speedMpsP50());
                        BottleneckInfo bn = computeBottleneck(m, netPres, speedDrop, 0);
                        CounterflowInfo cf = computeCounterflow(m);
                        timeline.add(new ZoneAnalyticsTimelinePoint(
                                f.offsetSec(),
                                m.densityPerSqM(),
                                m.flowInPerMin(),
                                m.flowOutPerMin(),
                                netPres,
                                m.speedMpsP50(),
                                speedDrop,
                                m.queueLength(),
                                round(m.movementInstability()),
                                bn.bottleneckScore(),
                                bn.isBottleneck(),
                                cf.isCounterflowDetected()));
                    });
        }

        return new ZoneAnalyticsDetailResponse(
                Instant.now().toString(),
                eventId,
                state.scenarioId(),
                state.currentOffsetSec(),
                analytics,
                timeline);
    }

    private ZoneAnalytics computeZoneAnalytics(ZoneMetric current, ZoneMetric prev) {
        // Density Trend
        double prevDensity = prev != null ? prev.densityPerSqM() : current.densityPerSqM();
        double densityDelta = round(current.densityPerSqM() - prevDensity);
        TrendDirection densityTrend;
        if (densityDelta > 0.05) {
            densityTrend = TrendDirection.RISING;
        } else if (densityDelta < -0.05) {
            densityTrend = TrendDirection.FALLING;
        } else {
            densityTrend = TrendDirection.STABLE;
        }

        // Inflow Trend
        int prevInflow = prev != null ? prev.flowInPerMin() : current.flowInPerMin();
        int inflowDelta = current.flowInPerMin() - prevInflow;
        TrendDirection inflowTrend;
        if (inflowDelta > 2) {
            inflowTrend = TrendDirection.RISING;
        } else if (inflowDelta < -2) {
            inflowTrend = TrendDirection.FALLING;
        } else {
            inflowTrend = TrendDirection.STABLE;
        }

        // Outflow Trend
        int prevOutflow = prev != null ? prev.flowOutPerMin() : current.flowOutPerMin();
        int outflowDelta = current.flowOutPerMin() - prevOutflow;
        TrendDirection outflowTrend;
        if (outflowDelta > 2) {
            outflowTrend = TrendDirection.RISING;
        } else if (outflowDelta < -2) {
            outflowTrend = TrendDirection.FALLING;
        } else {
            outflowTrend = TrendDirection.STABLE;
        }

        // Net pressure
        int netPressure = current.flowInPerMin() - current.flowOutPerMin();

        // Speed drop percentage
        double speedDropPct = computeSpeedDropPct(current.speedMpsP50());

        // Queue growth
        int prevQueue = prev != null ? prev.queueLength() : current.queueLength();
        int queueGrowth = current.queueLength() - prevQueue;
        TrendDirection queueTrend;
        if (queueGrowth > 0) {
            queueTrend = TrendDirection.RISING;
        } else if (queueGrowth < 0) {
            queueTrend = TrendDirection.FALLING;
        } else {
            queueTrend = TrendDirection.STABLE;
        }

        // Bottleneck Detection
        BottleneckInfo bottleneck = computeBottleneck(current, netPressure, speedDropPct, queueGrowth);

        // Counterflow Detection
        CounterflowInfo counterflow = computeCounterflow(current);

        // Movement Instability
        double movementInstability = round(current.movementInstability());
        RiskLevel instabilityLevel = computeInstabilityLevel(movementInstability);

        // Combined explanations
        List<String> combinedReasons = new ArrayList<>();
        if (bottleneck.isBottleneck()) {
            combinedReasons.addAll(bottleneck.reasons());
        }
        if (counterflow.isCounterflowDetected()) {
            combinedReasons.addAll(counterflow.reasons());
        }
        if (queueTrend == TrendDirection.RISING && queueGrowth > 2) {
            combinedReasons.add("queue_rapidly_growing");
        }
        if (densityTrend == TrendDirection.RISING && current.densityPerSqM() >= 2.0) {
            combinedReasons.add("density_rapidly_rising");
        }
        if (combinedReasons.isEmpty()) {
            combinedReasons.add("nominal_crowd_dynamics");
        }

        return new ZoneAnalytics(
                current.zoneId(),
                current.densityPerSqM(),
                round(current.densityPressure()),
                densityTrend,
                densityDelta,
                current.flowInPerMin(),
                inflowTrend,
                current.flowOutPerMin(),
                outflowTrend,
                netPressure,
                current.speedMpsP50(),
                speedDropPct,
                current.queueLength(),
                queueGrowth,
                queueTrend,
                movementInstability,
                instabilityLevel,
                bottleneck,
                counterflow,
                current.confidence(),
                current.dataQuality(),
                combinedReasons);
    }

    private double computeSpeedDropPct(double speedMps) {
        if (speedMps >= FREE_FLOW_BASELINE_SPEED) {
            return 0.0;
        }
        double drop = ((FREE_FLOW_BASELINE_SPEED - speedMps) / FREE_FLOW_BASELINE_SPEED) * 100.0;
        return round(Math.max(0.0, drop));
    }

    private BottleneckInfo computeBottleneck(
            ZoneMetric metric, int netPressure, double speedDropPct, int queueGrowth) {
        double rawPressure = metric.bottleneckPressure();
        double netPressureRatio = Math.max(0.0, Math.min(1.0, netPressure / 80.0));
        double speedDropRatio = Math.max(0.0, Math.min(1.0, speedDropPct / 100.0));
        double densityRatio = Math.max(0.0, Math.min(1.0, metric.densityPressure()));

        // Weighted bottleneck composite score
        double score = (rawPressure * 0.40) + (netPressureRatio * 0.25) + (speedDropRatio * 0.20) + (densityRatio * 0.15);
        score = round(Math.min(1.0, Math.max(0.0, score)));

        RiskLevel severity;
        boolean isBottleneck;
        if (score >= 0.70 || rawPressure >= 0.75) {
            severity = RiskLevel.CRITICAL;
            isBottleneck = true;
        } else if (score >= 0.50 || rawPressure >= 0.55) {
            severity = RiskLevel.HIGH;
            isBottleneck = true;
        } else if (score >= 0.35 || rawPressure >= 0.35) {
            severity = RiskLevel.MODERATE;
            isBottleneck = false;
        } else {
            severity = RiskLevel.LOW;
            isBottleneck = false;
        }

        // Explanations for WHY it is risky
        List<String> reasons = new ArrayList<>();
        if (netPressure > 10) {
            reasons.add("inflow_exceeds_outflow");
        }
        if (metric.densityPerSqM() >= 2.5 || metric.densityPressure() >= 0.6) {
            reasons.add("high_density_accumulation");
        }
        if (speedDropPct >= 30.0) {
            reasons.add("severe_speed_drop");
        }
        if (metric.queueLength() >= 10 || queueGrowth > 0) {
            reasons.add("growing_queue");
        }
        if (rawPressure >= 0.70) {
            reasons.add("bottleneck_pressure_critical");
        }
        if (metric.routeAvailability() < 0.5) {
            reasons.add("restricted_route_capacity");
        }
        if (reasons.isEmpty()) {
            reasons.add("flow_within_normal_limits");
        }

        Map<String, Double> contributingFactors = new LinkedHashMap<>();
        contributingFactors.put("bottleneck_pressure", round(rawPressure));
        contributingFactors.put("net_pressure_ratio", round(netPressureRatio));
        contributingFactors.put("speed_drop_ratio", round(speedDropRatio));
        contributingFactors.put("density_pressure", round(densityRatio));

        return new BottleneckInfo(
                metric.zoneId(),
                isBottleneck,
                severity,
                score,
                reasons,
                contributingFactors);
    }

    private CounterflowInfo computeCounterflow(ZoneMetric metric) {
        double opposing = metric.opposingMovement();
        RiskLevel severity;
        boolean isDetected;
        List<String> reasons = new ArrayList<>();

        if (opposing >= 0.60) {
            severity = RiskLevel.CRITICAL;
            isDetected = true;
            reasons.add("critical_counterflow_conflict");
            reasons.add("opposing_movement_severe");
        } else if (opposing >= 0.35) {
            severity = RiskLevel.HIGH;
            isDetected = true;
            reasons.add("opposing_flow_detected");
            reasons.add("head_on_crowd_turbulence");
        } else if (opposing >= 0.20) {
            severity = RiskLevel.MODERATE;
            isDetected = true;
            reasons.add("minor_opposing_flow_detected");
        } else {
            severity = RiskLevel.LOW;
            isDetected = false;
            reasons.add("unidirectional_flow");
        }

        return new CounterflowInfo(
                metric.zoneId(),
                isDetected,
                severity,
                round(opposing),
                reasons);
    }

    private RiskLevel computeInstabilityLevel(double instability) {
        if (instability >= 0.70) {
            return RiskLevel.CRITICAL;
        }
        if (instability >= 0.50) {
            return RiskLevel.HIGH;
        }
        if (instability >= 0.30) {
            return RiskLevel.MODERATE;
        }
        return RiskLevel.LOW;
    }

    private double round(double val) {
        return Math.round(val * 1000.0) / 1000.0;
    }
}
