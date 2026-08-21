package com.crowdshield.service;

import com.crowdshield.model.VenueGraph;
import org.springframework.stereotype.Service;

@Service
public class VenueService {
    private final DemoDataService demoDataService;

    public VenueService(DemoDataService demoDataService) {
        this.demoDataService = demoDataService;
    }

    public VenueGraph getVenue() {
        return demoDataService.getVenueGraph();
    }
}
