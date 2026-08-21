package com.crowdshield.service;

import com.crowdshield.model.RiskResponse;
import com.crowdshield.web.SimulationActionRequest;
import com.crowdshield.web.SimulationRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SimulationService {
    private final RiskScoringService riskScoringService;

    public SimulationService(RiskScoringService riskScoringService) {
        this.riskScoringService = riskScoringService;
    }

    public Map<String, Object> run(SimulationRequest request, String eventId) {
        RiskResponse current = riskScoringService.calculateCurrentRisk();
        double baseline = current.overallRisk().score();
        double projected = projectRisk(baseline, request.actions());

        return Map.of(
                "timestamp", Instant.now().toString(),
                "eventId", eventId,
                "simulationId", "sim-" + UUID.randomUUID(),
                "heuristic", true,
                "baseline", Map.of("peakRisk", baseline, "timeToPeakSec", 180),
                "projected", Map.of("peakRisk", projected, "timeToPeakSec", 220),
                "delta", Map.of("peakRisk", round(projected - baseline), "estimatedClearanceSec", -120),
                "recommendation", "Heuristic simulation suggests reduced risk trajectory for selected actions.");
    }

    private double projectRisk(double baseline, List<SimulationActionRequest> actions) {
        double adjusted = baseline;
        for (SimulationActionRequest action : actions) {
            if ("CLOSE_GATE".equalsIgnoreCase(action.type())) {
                adjusted -= 0.07;
            }
            if ("OPEN_ROUTE".equalsIgnoreCase(action.type())) {
                adjusted -= 0.10;
            }
        }
        adjusted = Math.max(0.0, adjusted);
        return round(adjusted);
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
