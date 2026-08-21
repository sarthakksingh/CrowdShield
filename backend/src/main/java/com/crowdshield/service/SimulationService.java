package com.crowdshield.service;

import com.crowdshield.model.PlaybackState;
import com.crowdshield.model.RiskResponse;
import com.crowdshield.model.ZoneMetric;
import com.crowdshield.model.ZoneRisk;
import com.crowdshield.web.SimulationActionRequest;
import com.crowdshield.web.SimulationRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SimulationService {
    private static final String DISCLAIMER =
            "Prototype heuristic safety scoring for demonstration purposes only; not certified life-safety software.";

    private final RiskScoringService riskScoringService;
    private final ScenarioPlaybackService playbackService;

    public SimulationService(
            RiskScoringService riskScoringService,
            ScenarioPlaybackService playbackService) {
        this.riskScoringService = riskScoringService;
        this.playbackService = playbackService;
    }

    public Map<String, Object> run(SimulationRequest request, String eventId) {
        PlaybackState playbackState = playbackService.getState();
        List<ZoneMetric> liveMetrics = playbackState.currentZoneMetrics();
        if (liveMetrics == null || liveMetrics.isEmpty()) {
            liveMetrics = createFallbackMetrics();
        }

        // 1. Calculate Baseline Risk (Stateless evaluation on unmodified copy)
        RiskResponse baselineRisk = riskScoringService.evaluateMetricsStateless(new ArrayList<>(liveMetrics));
        double baselineOverall = baselineRisk.overallRisk().score();
        double baselinePeak = baselineRisk.zoneRisks().stream()
                .mapToDouble(ZoneRisk::score)
                .max()
                .orElse(baselineOverall);

        // 2. Clone state into isolated working map for simulation
        Map<String, ZoneMetric> workingMetrics = new LinkedHashMap<>();
        for (ZoneMetric m : liveMetrics) {
            workingMetrics.put(m.zoneId(), m);
        }

        List<String> riskTransferNotes = new ArrayList<>();

        // 3. Apply Actions to working copy
        for (SimulationActionRequest action : request.actions()) {
            applyAction(action, workingMetrics, riskTransferNotes);
        }

        // 4. Calculate Projected Risk on modified working copy
        List<ZoneMetric> projectedMetricList = new ArrayList<>(workingMetrics.values());
        RiskResponse projectedRisk = riskScoringService.evaluateMetricsStateless(projectedMetricList);
        double projectedOverall = projectedRisk.overallRisk().score();
        double projectedPeak = projectedRisk.zoneRisks().stream()
                .mapToDouble(ZoneRisk::score)
                .max()
                .orElse(projectedOverall);

        // 5. Build Zone Impacts breakdown
        Map<String, ZoneRisk> baselineZoneMap = new LinkedHashMap<>();
        baselineRisk.zoneRisks().forEach(z -> baselineZoneMap.put(z.zoneId(), z));

        List<Map<String, Object>> zoneImpacts = new ArrayList<>();
        for (ZoneRisk projZone : projectedRisk.zoneRisks()) {
            ZoneRisk baseZone = baselineZoneMap.get(projZone.zoneId());
            double baseScore = baseZone != null ? baseZone.score() : projZone.score();
            double deltaScore = round(projZone.score() - baseScore);

            String status;
            String note;
            if (deltaScore <= -0.04) {
                status = "IMPROVED";
                note = String.format("Risk reduced by %.3f (from %.3f to %.3f) due to intervention relief.", -deltaScore, baseScore, projZone.score());
            } else if (deltaScore >= 0.03) {
                status = "TRANSFERRED_RISK";
                note = String.format("Transferred pressure increased risk by +%.3f (from %.3f to %.3f).", deltaScore, baseScore, projZone.score());
                if (riskTransferNotes.stream().noneMatch(n -> n.contains(projZone.zoneId()))) {
                    riskTransferNotes.add(String.format("Transferred crowd pressure detected in %s (+%.3f risk delta).", projZone.zoneId(), deltaScore));
                }
            } else {
                status = "UNCHANGED";
                note = "Nominal variation within steady state tolerance.";
            }

            zoneImpacts.add(Map.of(
                    "zoneId", projZone.zoneId(),
                    "baselineScore", baseScore,
                    "projectedScore", projZone.score(),
                    "delta", deltaScore,
                    "status", status,
                    "notes", note));
        }

        // 6. Compute Verdict and Recommendation Text
        double overallDelta = round(projectedOverall - baselineOverall);
        double peakDelta = round(projectedPeak - baselinePeak);

        String verdict;
        if (overallDelta <= -0.04 || peakDelta <= -0.05) {
            verdict = "IMPROVED";
        } else if (overallDelta >= 0.04 || peakDelta >= 0.05) {
            verdict = "WORSENED";
        } else {
            verdict = "NO_SIGNIFICANT_CHANGE";
        }

        String recommendation;
        if ("IMPROVED".equals(verdict)) {
            recommendation = String.format(
                    "Simulation projects overall risk reduction of %.3f (peak from %.3f to %.3f). %s",
                    -overallDelta, baselinePeak, projectedPeak,
                    riskTransferNotes.isEmpty() ? "No significant adverse risk transfer observed." : "Monitor upstream holding zones for secondary queue formation.");
        } else if ("WORSENED".equals(verdict)) {
            recommendation = "Simulation indicates proposed action increases overall risk or creates adverse secondary choke points.";
        } else {
            recommendation = "Simulation projects negligible impact on critical risk metrics for the selected horizon.";
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("timestamp", Instant.now().toString());
        result.put("eventId", eventId);
        result.put("simulationId", "sim-" + UUID.randomUUID().toString().substring(0, 8));
        result.put("scenarioName", request.scenarioName());
        result.put("heuristic", true);
        result.put("disclaimer", DISCLAIMER);
        result.put("baseline", Map.of(
                "peakRisk", baselinePeak,
                "overallScore", baselineOverall,
                "level", baselineRisk.overallRisk().level().name(),
                "timeToPeakSec", 180,
                "zoneRisks", baselineRisk.zoneRisks()));
        result.put("projected", Map.of(
                "peakRisk", projectedPeak,
                "overallScore", projectedOverall,
                "level", projectedRisk.overallRisk().level().name(),
                "timeToPeakSec", 240,
                "zoneRisks", projectedRisk.zoneRisks()));
        result.put("delta", Map.of(
                "peakRisk", peakDelta,
                "overallScore", overallDelta,
                "estimatedClearanceSec", -120));
        result.put("zoneImpacts", zoneImpacts);
        result.put("riskTransferNotes", riskTransferNotes);
        result.put("verdict", verdict);
        result.put("recommendation", recommendation);

        return result;
    }

    private void applyAction(
            SimulationActionRequest action,
            Map<String, ZoneMetric> metrics,
            List<String> riskTransferNotes) {
        String type = action.type().toUpperCase();
        String target = action.targetId().toLowerCase();

        if (type.contains("CLOSE_GATE") || type.contains("RESTRICT_GATE")) {
            // Relieves downstream corridor
            ZoneMetric corridor = findZone(metrics, "corridor", "z-corridor-1");
            if (corridor != null) {
                int newFlowIn = (int) Math.max(10, corridor.flowInPerMin() * 0.55);
                double newDensityPres = clamp(corridor.densityPressure() - 0.22);
                double newBottleneckPres = clamp(corridor.bottleneckPressure() - 0.30);
                double newDensity = Math.max(1.0, corridor.densityPerSqM() * 0.70);
                double newSpeed = Math.min(1.5, corridor.speedMpsP50() + 0.30);
                int newQueue = (int) Math.max(2, corridor.queueLength() * 0.60);

                metrics.put(corridor.zoneId(), copyMetricWith(
                        corridor, newDensityPres, clamp(corridor.inflowOutflowImbalance() - 0.35),
                        corridor.movementInstability(), corridor.opposingMovement(), newBottleneckPres,
                        corridor.routeAvailability(), newDensity, newFlowIn, corridor.flowOutPerMin(),
                        newSpeed, newQueue));
            }

            // Upstream entry absorbs queue holding pressure (Transferred Risk)
            ZoneMetric entry = findZone(metrics, "entry", target);
            if (entry != null) {
                int shiftedQueue = entry.queueLength() + 10;
                double entryDensityPres = clamp(entry.densityPressure() + 0.08);
                metrics.put(entry.zoneId(), copyMetricWith(
                        entry, entryDensityPres, entry.inflowOutflowImbalance(),
                        entry.movementInstability(), entry.opposingMovement(), entry.bottleneckPressure(),
                        entry.routeAvailability(), entry.densityPerSqM() + 0.3, entry.flowInPerMin(), entry.flowOutPerMin(),
                        entry.speedMpsP50(), shiftedQueue));

                riskTransferNotes.add(String.format(
                        "Restricting entry at %s shifts queue accumulation upstream to %s (+%d queue length).",
                        target, entry.zoneId(), 10));
            }
        } else if (type.contains("OPEN_ROUTE") || type.contains("OPEN_EXIT")) {
            // Corridor flowOut increases, bottleneck drops
            ZoneMetric corridor = findZone(metrics, "corridor", "z-corridor-1");
            if (corridor != null) {
                int newFlowOut = (int) (corridor.flowOutPerMin() * 1.50);
                double newBottleneckPres = clamp(corridor.bottleneckPressure() - 0.35);
                double newRouteAvail = clamp(corridor.routeAvailability() + 0.40);
                double newDensityPres = clamp(corridor.densityPressure() - 0.18);
                double newSpeed = Math.min(1.5, corridor.speedMpsP50() + 0.35);
                int newQueue = (int) Math.max(2, corridor.queueLength() * 0.50);

                metrics.put(corridor.zoneId(), copyMetricWith(
                        corridor, newDensityPres, clamp(corridor.inflowOutflowImbalance() - 0.30),
                        corridor.movementInstability(), corridor.opposingMovement(), newBottleneckPres,
                        newRouteAvail, Math.max(1.0, corridor.densityPerSqM() - 0.8), corridor.flowInPerMin(),
                        newFlowOut, newSpeed, newQueue));
            }

            // Exit zone reflects increased throughput
            ZoneMetric exit = findZone(metrics, "exit", "z-exit-east");
            if (exit != null) {
                int newExitFlowOut = (int) (exit.flowOutPerMin() * 1.35);
                metrics.put(exit.zoneId(), copyMetricWith(
                        exit, exit.densityPressure(), exit.inflowOutflowImbalance(),
                        exit.movementInstability(), exit.opposingMovement(), exit.bottleneckPressure(),
                        clamp(exit.routeAvailability() + 0.20), exit.densityPerSqM(), exit.flowInPerMin(),
                        newExitFlowOut, exit.speedMpsP50(), exit.queueLength()));
            }
        } else if (type.contains("REDIRECT_INFLOW")) {
            // Reduces opposing movement and turbulence in target
            ZoneMetric zone = findZone(metrics, target, "z-corridor-1");
            if (zone != null) {
                double newOpposing = clamp(zone.opposingMovement() * 0.25);
                double newInstability = clamp(zone.movementInstability() * 0.45);
                metrics.put(zone.zoneId(), copyMetricWith(
                        zone, zone.densityPressure(), zone.inflowOutflowImbalance(),
                        newInstability, newOpposing, zone.bottleneckPressure(),
                        zone.routeAvailability(), zone.densityPerSqM(), zone.flowInPerMin(), zone.flowOutPerMin(),
                        zone.speedMpsP50(), zone.queueLength()));
            }
        } else if (type.contains("DEPLOY_PERSONNEL")) {
            // Personnel assists queue clearance and stabilizes movement
            ZoneMetric zone = findZone(metrics, target, "z-corridor-1");
            if (zone != null) {
                double newInstability = clamp(zone.movementInstability() * 0.50);
                int newQueue = (int) Math.max(1, zone.queueLength() * 0.65);
                metrics.put(zone.zoneId(), copyMetricWith(
                        zone, clamp(zone.densityPressure() - 0.08), zone.inflowOutflowImbalance(),
                        newInstability, zone.opposingMovement(), zone.bottleneckPressure(),
                        zone.routeAvailability(), zone.densityPerSqM(), zone.flowInPerMin(), zone.flowOutPerMin(),
                        Math.min(1.5, zone.speedMpsP50() + 0.15), newQueue));
            }
        }
    }

    private ZoneMetric findZone(Map<String, ZoneMetric> metrics, String keyword, String fallbackKey) {
        for (Map.Entry<String, ZoneMetric> entry : metrics.entrySet()) {
            if (entry.getKey().toLowerCase().contains(keyword.toLowerCase())) {
                return entry.getValue();
            }
        }
        return metrics.get(fallbackKey);
    }

    private ZoneMetric copyMetricWith(
            ZoneMetric original,
            double densityPressure,
            double inflowOutflowImbalance,
            double movementInstability,
            double opposingMovement,
            double bottleneckPressure,
            double routeAvailability,
            double densityPerSqM,
            int flowInPerMin,
            int flowOutPerMin,
            double speedMpsP50,
            int queueLength) {
        return new ZoneMetric(
                original.zoneId(),
                densityPressure,
                inflowOutflowImbalance,
                movementInstability,
                opposingMovement,
                bottleneckPressure,
                routeAvailability,
                densityPerSqM,
                flowInPerMin,
                flowOutPerMin,
                speedMpsP50,
                queueLength,
                original.occupancyPct(),
                original.dataQuality(),
                original.confidence(),
                original.reasons());
    }

    private List<ZoneMetric> createFallbackMetrics() {
        return List.of(
                new ZoneMetric("z-entry-a", 0.5, 0.35, 0.25, 0.12, 0.35, 0.70, 1.8, 80, 65, 1.1, 8, 52, "GOOD", 0.89, List.of("nominal")),
                new ZoneMetric("z-corridor-1", 0.85, 0.78, 0.68, 0.58, 0.88, 0.35, 3.8, 115, 48, 0.65, 30, 91, "GOOD", 0.89, List.of("congested")),
                new ZoneMetric("z-open-yard", 0.38, 0.28, 0.18, 0.08, 0.22, 0.75, 1.4, 48, 46, 1.2, 5, 45, "GOOD", 0.85, List.of("open")),
                new ZoneMetric("z-exit-east", 0.25, 0.20, 0.15, 0.05, 0.15, 0.90, 1.0, 45, 50, 1.3, 2, 30, "GOOD", 0.90, List.of("free_flow")));
    }

    private double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
