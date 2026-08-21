package com.crowdshield.model;

import java.util.List;

public record ZoneDefinition(
        String zoneId,
        String name,
        int capacity,
        List<List<Double>> polygon) {}
