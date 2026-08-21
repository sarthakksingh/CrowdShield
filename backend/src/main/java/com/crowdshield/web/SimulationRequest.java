package com.crowdshield.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record SimulationRequest(
        @NotBlank String eventId,
        @NotBlank String scenarioName,
        @NotEmpty List<@Valid SimulationActionRequest> actions,
        @Min(1) int horizonSec) {}
