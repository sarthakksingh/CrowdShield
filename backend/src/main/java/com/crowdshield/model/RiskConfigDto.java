package com.crowdshield.model;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record RiskConfigDto(
        @NotNull Map<String, Double> thresholds,
        @NotNull Map<String, Double> weights,
        int hysteresisHoldSeconds,
        int persistenceRequiredFrames,
        String disclaimer) {}
