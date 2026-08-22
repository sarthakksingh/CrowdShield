package com.crowdshield.service;

import com.crowdshield.model.ForecastResponse;
import com.crowdshield.model.Frame;
import com.crowdshield.model.HorizonRiskForecast;
import com.crowdshield.model.OverallForecast;
import com.crowdshield.model.PlaybackState;
import com.crowdshield.model.RiskLevel;
import com.crowdshield.model.ZoneForecast;
import com.crowdshield.model.ZoneMetric;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ShortHorizonForecastService {
    private static final List<Integer> HORIZONS_SEC = List.of(30, 60, 180, 300);
    private static final String DISCLAIMER =
            "Prototype heuristic safety scoring for demonstration purposes only; not certified life-safety software.";

    private final RiskScoringService riskScoringService;
    private final ScenarioPlaybackService playbackService;
    private final EventStateService eventStateService;

    public ShortHorizonForecastService(
            RiskScoringService riskScoringService,
            ScenarioPlaybackService playbackService,
            EventStateService eventStateService) {
        this.riskScoringService = riskScoringService;
        this.playbackService = playbackService;
        this.eventStateService = eventStateService;
    }

    public ForecastResponse getCurrentForecast() {
        PlaybackState state = playbackService.getState();
        String eventId = eventStateService.getEventId();
        List<Frame> frames = playbackService.getFramesUpToCurrentOffset();
        List<ZoneMetric> currentMetrics = state.currentZoneMetrics();

        if (currentMetrics == null || currentMetrics.isEmpty()) {
            OverallForecast emptyOverall = new OverallForecast(
                    0.0, RiskLevel.LOW, Map.of(), "STABLE", 1.0, 999, "> 15 min / stable", List.of("no_data"));
            return new ForecastResponse(
                    Instant.now().toString(), eventId, state.scenarioId(), state.currentOffsetSec(),
                    HORIZONS_SEC, emptyOverall, List.of(), DISCLAIMER);
        }

        List<ZoneForecast> zoneForecasts = new ArrayList<>();
        for (ZoneMetric m : currentMetrics) {
            zoneForecasts.add(forecastZone(m.zoneId(), frames, m));
        }

        OverallForecast overall = computeOverallForecast(zoneForecasts, currentMetrics);

        return new ForecastResponse(
                Instant.now().toString(),
                eventId,
                state.scenarioId(),
                state.currentOffsetSec(),
                HORIZONS_SEC,
                overall,
                zoneForecasts,
                DISCLAIMER);
    }

    private ZoneForecast forecastZone(String zoneId, List<Frame> frames, ZoneMetric currentMetric) {
        double currentScore = riskScoringService.score(currentMetric).score();
        RiskLevel currentLevel = riskScoringService.levelFor(currentScore);

        // Collect historical (offsetSec, score) series
        List<Point> series = new ArrayList<>();
        for (Frame f : frames) {
            f.zoneMetrics().stream()
                    .filter(zm -> zm.zoneId().equalsIgnoreCase(zoneId))
                    .findFirst()
                    .ifPresent(zm -> {
                        double sc = riskScoringService.score(zm).score();
                        series.add(new Point(f.offsetSec(), sc));
                    });
        }

        // Limit to recent history window (last 6-8 frames) for agile responsiveness
        int windowSize = Math.min(series.size(), 8);
        List<Point> recentSeries = series.size() > windowSize
                ? series.subList(series.size() - windowSize, series.size())
                : series;

        // 1. Linear Regression
        LinearResult linear = computeLinearRegression(recentSeries, currentScore);

        // 2. Exponential Smoothing (Holt's linear trend)
        ExpSmoothingResult expSmooth = computeExpSmoothing(recentSeries, currentScore);

        // 3. Compare methods
        boolean methodsAgree = linear.trend.equals(expSmooth.trend);
        String dominantTrend = determineDominantTrend(linear, expSmooth);
        double baseConfidence = currentMetric.confidence();
        double confidence = methodsAgree
                ? Math.min(0.95, baseConfidence + 0.06)
                : Math.max(0.60, baseConfidence - 0.18);

        // 4. Build horizon forecasts (30s, 60s, 180s, 300s)
        Map<Integer, HorizonRiskForecast> horizons = new LinkedHashMap<>();
        for (int h : HORIZONS_SEC) {
            double projectedLinear = clamp(currentScore + linear.slopePerSec * h);
            double steps = h / 10.0;
            double projectedExp = clamp(expSmooth.level + expSmooth.trendPerStep * steps);

            double selectedScore;
            if (methodsAgree) {
                selectedScore = round(0.50 * projectedLinear + 0.50 * projectedExp);
            } else {
                // Dampened conservative projection toward current score
                selectedScore = round(0.60 * currentScore + 0.25 * projectedLinear + 0.15 * projectedExp);
            }

            RiskLevel level = riskScoringService.levelFor(selectedScore);
            double delta = round(selectedScore - currentScore);
            horizons.put(h, new HorizonRiskForecast(h, selectedScore, level, delta));
        }

        // 5. Estimate time-to-critical horizon string
        String horizonEstimate = estimateZoneHorizon(currentScore, dominantTrend, linear.slopePerSec);

        List<String> reasons = new ArrayList<>();
        if ("RISING".equals(dominantTrend)) {
            reasons.add("historical_upward_trajectory");
            if (linear.slopePerSec > 0.0015) {
                reasons.add("steep_risk_acceleration");
            }
        } else if ("FALLING".equals(dominantTrend)) {
            reasons.add("positive_recovery_trajectory");
        } else {
            reasons.add("stable_steady_state");
        }

        if (!methodsAgree) {
            reasons.add("method_divergence_damped");
        }

        return new ZoneForecast(
                zoneId,
                round(currentScore),
                currentLevel,
                horizons,
                dominantTrend,
                "ENSEMBLE_CONSERVATIVE_BASELINE",
                methodsAgree,
                round(confidence),
                horizonEstimate,
                reasons);
    }

    private OverallForecast computeOverallForecast(List<ZoneForecast> zoneForecasts, List<ZoneMetric> metrics) {
        double currentPeak = zoneForecasts.stream().mapToDouble(ZoneForecast::currentScore).max().orElse(0.0);
        double currentAvg = zoneForecasts.stream().mapToDouble(ZoneForecast::currentScore).average().orElse(0.0);
        double currentOverall = round(currentPeak * 0.65 + currentAvg * 0.35);
        RiskLevel currentLevel = riskScoringService.levelFor(currentOverall);

        Map<Integer, HorizonRiskForecast> overallHorizons = new LinkedHashMap<>();
        for (int h : HORIZONS_SEC) {
            double horizonPeak = zoneForecasts.stream()
                    .mapToDouble(z -> z.horizons().get(h).score())
                    .max()
                    .orElse(0.0);
            double horizonAvg = zoneForecasts.stream()
                    .mapToDouble(z -> z.horizons().get(h).score())
                    .average()
                    .orElse(0.0);
            double score = round(horizonPeak * 0.65 + horizonAvg * 0.35);
            RiskLevel level = riskScoringService.levelFor(score);
            double delta = round(score - currentOverall);
            overallHorizons.put(h, new HorizonRiskForecast(h, score, level, delta));
        }

        // Overall trend
        double delta300 = overallHorizons.get(300).deltaFromCurrent();
        String trend;
        if (delta300 > 0.04) {
            trend = "RISING";
        } else if (delta300 < -0.04) {
            trend = "FALLING";
        } else {
            trend = "STABLE";
        }

        double confidence = zoneForecasts.stream().mapToDouble(ZoneForecast::confidence).average().orElse(0.85);

        // Time to critical
        int timeToCriticalSec = 999;
        if ("RISING".equals(trend) && currentOverall < 0.75 && delta300 > 0) {
            double ratePerSec = delta300 / 300.0;
            if (ratePerSec > 0) {
                timeToCriticalSec = (int) Math.round((0.75 - currentOverall) / ratePerSec);
            }
        }

        String horizonEstimate = formatHorizonEstimate(timeToCriticalSec, trend);

        List<String> reasons = new ArrayList<>();
        if ("RISING".equals(trend)) {
            reasons.add("overall_congestion_expanding");
        } else if ("FALLING".equals(trend)) {
            reasons.add("overall_system_decompressing");
        } else {
            reasons.add("overall_flow_steady");
        }

        return new OverallForecast(
                currentOverall,
                currentLevel,
                overallHorizons,
                trend,
                round(confidence),
                timeToCriticalSec,
                horizonEstimate,
                reasons);
    }

    private LinearResult computeLinearRegression(List<Point> points, double fallback) {
        if (points == null || points.size() < 2) {
            return new LinearResult(0.0, fallback, "STABLE");
        }

        int n = points.size();
        double sumT = 0, sumY = 0, sumTT = 0, sumTY = 0;
        for (Point p : points) {
            sumT += p.t;
            sumY += p.y;
            sumTT += p.t * p.t;
            sumTY += p.t * p.y;
        }

        double denominator = (n * sumTT - sumT * sumT);
        if (Math.abs(denominator) < 1e-6) {
            return new LinearResult(0.0, fallback, "STABLE");
        }

        double slope = (n * sumTY - sumT * sumY) / denominator;
        double intercept = (sumY - slope * sumT) / n;

        String trend;
        if (slope > 0.0006) {
            trend = "RISING";
        } else if (slope < -0.0006) {
            trend = "FALLING";
        } else {
            trend = "STABLE";
        }

        return new LinearResult(slope, intercept, trend);
    }

    private ExpSmoothingResult computeExpSmoothing(List<Point> points, double fallback) {
        if (points == null || points.isEmpty()) {
            return new ExpSmoothingResult(fallback, 0.0, "STABLE");
        }
        if (points.size() == 1) {
            return new ExpSmoothingResult(points.get(0).y, 0.0, "STABLE");
        }

        double alpha = 0.40;
        double beta = 0.20;

        double level = points.get(0).y;
        double trend = points.get(1).y - points.get(0).y;

        for (int i = 1; i < points.size(); i++) {
            double y = points.get(i).y;
            double prevLevel = level;
            double prevTrend = trend;
            level = alpha * y + (1 - alpha) * (prevLevel + prevTrend);
            trend = beta * (level - prevLevel) + (1 - beta) * prevTrend;
        }

        String trendStr;
        if (trend > 0.004) {
            trendStr = "RISING";
        } else if (trend < -0.004) {
            trendStr = "FALLING";
        } else {
            trendStr = "STABLE";
        }

        return new ExpSmoothingResult(level, trend, trendStr);
    }

    private String determineDominantTrend(LinearResult lin, ExpSmoothingResult exp) {
        if (lin.trend.equals(exp.trend)) {
            return lin.trend;
        }
        if ("RISING".equals(lin.trend) || "RISING".equals(exp.trend)) {
            return "RISING";
        }
        if ("FALLING".equals(lin.trend) || "FALLING".equals(exp.trend)) {
            return "FALLING";
        }
        return "STABLE";
    }

    private String estimateZoneHorizon(double score, String trend, double slopePerSec) {
        if (score >= 0.75) {
            return "immediate (< 1 min)";
        }
        if (!"RISING".equals(trend) || slopePerSec <= 0) {
            return "> 15 min / stable";
        }

        double secToCrit = (0.75 - score) / slopePerSec;
        if (secToCrit <= 60) {
            return "immediate (< 1 min)";
        }
        if (secToCrit <= 120) {
            return "1-2 min";
        }
        if (secToCrit <= 240) {
            return "2-4 min";
        }
        if (secToCrit <= 360) {
            return "4-6 min";
        }
        return "> 15 min / stable";
    }

    private String formatHorizonEstimate(int sec, String trend) {
        if (!"RISING".equals(trend)) {
            return "> 15 min / stable";
        }
        if (sec <= 60) return "immediate (< 1 min)";
        if (sec <= 120) return "1-2 min";
        if (sec <= 240) return "2-4 min";
        if (sec <= 360) return "4-6 min";
        return "> 15 min / stable";
    }

    private double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private double round(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }

    private record Point(double t, double y) {}
    private record LinearResult(double slopePerSec, double intercept, String trend) {}
    private record ExpSmoothingResult(double level, double trendPerStep, String trend) {}
}
