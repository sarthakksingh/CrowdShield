package com.crowdshield.service;

import com.crowdshield.model.VenueGraph;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RouteService {
    private final DemoDataService demoDataService;

    public RouteService(DemoDataService demoDataService) {
        this.demoDataService = demoDataService;
    }

    public Map<String, Object> safestRoute(String fromZoneId, String toExitId, String eventId) {
        VenueGraph venue = demoDataService.getVenueGraph();
        String destination = toExitId == null || toExitId.isBlank() ? venue.exits().getFirst() : toExitId;
        String midpoint = "z-open-yard";

        return Map.of(
                "timestamp", Instant.now().toString(),
                "eventId", eventId,
                "routeId", "rt-" + UUID.randomUUID(),
                "fromZoneId", fromZoneId,
                "toExitId", destination,
                "estimatedMinutes", 4.5,
                "riskWeightedScore", 0.28,
                "steps", List.of(
                        Map.of("zoneId", fromZoneId, "instruction", "Move toward open yard through marked corridor."),
                        Map.of("zoneId", midpoint, "instruction", "Continue east and follow steward directions."),
                        Map.of("zoneId", destination, "instruction", "Exit through designated gate.")
                ),
                "validUntil", Instant.now().plus(60, ChronoUnit.SECONDS).toString());
    }
}
