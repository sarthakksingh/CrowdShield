package com.crowdshield.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record SimulationActionRequest(
        @NotBlank String type,
        @NotBlank String targetId,
        @Min(0) int atOffsetSec) {}
