package com.crowdshield.service;

import com.crowdshield.web.IncidentReportRequest;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class IncidentService {
    private final VenueService venueService;

    public IncidentService(VenueService venueService) {
        this.venueService = venueService;
    }

    public Map<String, Object> submit(IncidentReportRequest request, String eventId) {
        boolean zoneExists = venueService.getVenue().zones().stream()
                .anyMatch(z -> z.zoneId().equals(request.location().zoneId()));
        if (!zoneExists) {
            throw new IllegalArgumentException("location.zoneId unknown");
        }

        return Map.of(
                "timestamp", Instant.now().toString(),
                "eventId", eventId,
                "incidentId", "inc-" + UUID.randomUUID(),
                "status", "RECEIVED",
                "triageQueuePosition", 1);
    }
}
