package com.crowdshield.model;

import java.util.List;

public record Scenario(
        String scenarioId,
        String name,
        String description,
        String eventId,
        String venueId,
        int durationSec,
        int frameIntervalSec,
        List<Frame> frames) {}
