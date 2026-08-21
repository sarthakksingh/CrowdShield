package com.crowdshield.model;

public record Recommendation(
        String id,
        RecommendationActionType actionType,
        String targetZoneId,
        String targetType,
        String title,
        String reason,
        String expectedImpact,
        double confidence,
        RiskLevel urgency,
        String suggestedMessage,
        int rank) {}
