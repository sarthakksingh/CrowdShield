package com.crowdshield.model;

public record EdgeDefinition(
        String from,
        String to,
        double widthMeters,
        boolean oneWay) {}
