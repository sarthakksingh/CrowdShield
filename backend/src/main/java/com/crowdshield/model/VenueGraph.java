package com.crowdshield.model;

import java.util.List;

public record VenueGraph(
        String venueId,
        String name,
        List<ZoneDefinition> zones,
        List<EdgeDefinition> edges,
        List<String> exits) {}
