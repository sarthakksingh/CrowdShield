package com.crowdshield.model;

import java.util.List;
import java.util.Map;

public record OverallRisk(
        double score,
        RiskLevel level,
        String trend,
        double confidence,
        List<String> reasons,
        Map<String, Double> factorContributions,
        Map<String, String> factorDescriptions,
        String horizon,
        String holdUntil,
        String disclaimer) {

    public OverallRisk(
            double score,
            RiskLevel level,
            String trend,
            double confidence,
            List<String> reasons,
            Map<String, Double> factorContributions,
            String holdUntil) {
        this(score, level, trend, confidence, reasons, factorContributions, Map.of(), "N/A", holdUntil, "");
    }
}
