package com.crowdshield.model;

import java.util.List;

public record Frame(
        int offsetSec,
        List<ZoneMetric> zoneMetrics) {}
