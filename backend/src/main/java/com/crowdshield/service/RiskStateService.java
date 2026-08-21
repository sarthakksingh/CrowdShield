package com.crowdshield.service;

import com.crowdshield.config.CrowdShieldProperties;
import com.crowdshield.model.RiskLevel;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class RiskStateService {
    private static final double TREND_DELTA = 0.03;

    private final int holdSeconds;
    private final Map<String, RiskState> stateByKey = new ConcurrentHashMap<>();

    public RiskStateService(CrowdShieldProperties properties) {
        this.holdSeconds = properties.getHysteresisHoldSeconds();
    }

    public RiskSnapshot apply(String key, RiskLevel computedLevel, double score, Instant now) {
        RiskState current = stateByKey.computeIfAbsent(key, k -> new RiskState(computedLevel, now, score));

        RiskLevel effective = computedLevel;
        Instant holdUntil = current.holdUntil();
        if (computedLevel.ordinal() > current.level().ordinal()) {
            holdUntil = now.plusSeconds(holdSeconds);
            current = new RiskState(computedLevel, holdUntil, current.lastScore());
        } else if (computedLevel.ordinal() < current.level().ordinal()) {
            if (now.isBefore(current.holdUntil())) {
                effective = current.level();
            } else {
                holdUntil = now.plusSeconds(holdSeconds);
                current = new RiskState(computedLevel, holdUntil, current.lastScore());
            }
        }

        double lastScore = current.lastScore();
        String trend;
        if (score > lastScore + TREND_DELTA) {
            trend = "RISING";
        } else if (score < lastScore - TREND_DELTA) {
            trend = "FALLING";
        } else {
            trend = "STABLE";
        }

        stateByKey.put(key, new RiskState(effective, holdUntil, score));
        return new RiskSnapshot(effective, trend, holdUntil);
    }

    public void clear() {
        stateByKey.clear();
    }

    public record RiskSnapshot(RiskLevel level, String trend, Instant holdUntil) {}

    private record RiskState(RiskLevel level, Instant holdUntil, double lastScore) {}
}
