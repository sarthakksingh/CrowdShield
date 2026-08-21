package com.crowdshield.model;

public record AlertRecord(
        String alertId,
        String zoneId,
        String type,
        String severity,
        String status,
        String firstRaisedAt,
        String lastUpdatedAt,
        String dedupeKey,
        String message) {}
