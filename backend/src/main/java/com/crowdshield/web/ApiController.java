package com.crowdshield.web;

import com.crowdshield.model.RiskResponse;
import com.crowdshield.model.ScenarioData;
import com.crowdshield.model.VenueGraph;
import com.crowdshield.service.AlertService;
import com.crowdshield.service.EventStateService;
import com.crowdshield.service.IncidentService;
import com.crowdshield.service.RiskScoringService;
import com.crowdshield.service.RouteService;
import com.crowdshield.service.SimulationService;
import com.crowdshield.service.SseStreamService;
import com.crowdshield.service.VenueService;
import com.crowdshield.service.ZoneMetricsService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Validated
@RestController
@RequestMapping("/api")
public class ApiController {
    private final EventStateService eventStateService;
    private final VenueService venueService;
    private final ZoneMetricsService zoneMetricsService;
    private final RiskScoringService riskScoringService;
    private final AlertService alertService;
    private final SimulationService simulationService;
    private final IncidentService incidentService;
    private final RouteService routeService;
    private final SseStreamService sseStreamService;

    public ApiController(
            EventStateService eventStateService,
            VenueService venueService,
            ZoneMetricsService zoneMetricsService,
            RiskScoringService riskScoringService,
            AlertService alertService,
            SimulationService simulationService,
            IncidentService incidentService,
            RouteService routeService,
            SseStreamService sseStreamService) {
        this.eventStateService = eventStateService;
        this.venueService = venueService;
        this.zoneMetricsService = zoneMetricsService;
        this.riskScoringService = riskScoringService;
        this.alertService = alertService;
        this.simulationService = simulationService;
        this.incidentService = incidentService;
        this.routeService = routeService;
        this.sseStreamService = sseStreamService;
    }

    @GetMapping("/events/current")
    public Map<String, Object> currentEvent() {
        ScenarioData event = eventStateService.currentEvent();
        String eventId = eventStateService.getEventId();
        return Map.of(
                "timestamp", Instant.now().toString(),
                "eventId", eventId,
                "name", event.name(),
                "status", event.status(),
                "venueId", event.venueId(),
                "phase", event.phase(),
                "lastUpdateAt", Instant.now().toString());
    }

    @GetMapping("/venue")
    public Map<String, Object> venue() {
        VenueGraph venue = venueService.getVenue();
        String eventId = eventStateService.getEventId();
        return Map.of(
                "timestamp", Instant.now().toString(),
                "eventId", eventId,
                "venueId", venue.venueId(),
                "name", venue.name(),
                "zones", venue.zones(),
                "edges", venue.edges(),
                "exits", venue.exits());
    }

    @GetMapping("/zones")
    public Map<String, Object> zones() {
        String eventId = eventStateService.getEventId();
        return Map.of(
                "timestamp", Instant.now().toString(),
                "eventId", eventId,
                "zones", zoneMetricsService.getCurrentZoneMetrics());
    }

    @GetMapping("/risk/current")
    public Map<String, Object> currentRisk() {
        String eventId = eventStateService.getEventId();
        RiskResponse risk = riskScoringService.calculateCurrentRisk();
        return Map.of(
                "timestamp", Instant.now().toString(),
                "eventId", eventId,
                "overallRisk", risk.overallRisk(),
                "zoneRisks", risk.zoneRisks());
    }

    @GetMapping("/alerts")
    public Map<String, Object> alerts() {
        String eventId = eventStateService.getEventId();
        RiskResponse risk = riskScoringService.calculateCurrentRisk();
        return Map.of(
                "timestamp", Instant.now().toString(),
                "eventId", eventId,
                "alerts", alertService.refreshAndGetAlerts(risk));
    }

    @PostMapping("/simulations")
    public Map<String, Object> simulations(@Valid @RequestBody SimulationRequest request) {
        String eventId = eventStateService.getEventId();
        return simulationService.run(request, eventId);
    }

    @PostMapping("/incidents")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Object> incidents(@Valid @RequestBody IncidentReportRequest request) {
        String eventId = eventStateService.getEventId();
        return incidentService.submit(request, eventId);
    }

    @GetMapping("/routes/safest")
    public Map<String, Object> safestRoute(
            @RequestParam String fromZoneId,
            @RequestParam(required = false) String toExitId) {
        String eventId = eventStateService.getEventId();
        return routeService.safestRoute(fromZoneId, toExitId, eventId);
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        String eventId = eventStateService.getEventId();
        return sseStreamService.open(eventId);
    }
}
