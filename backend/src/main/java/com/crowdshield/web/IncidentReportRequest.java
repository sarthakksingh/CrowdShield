package com.crowdshield.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record IncidentReportRequest(
        @NotBlank String eventId,
        @NotBlank String reporterType,
        @NotNull @Valid IncidentLocation location,
        @NotBlank String category,
        @NotBlank String severity,
        @NotBlank String description,
        @NotBlank String reportedAt) {}
