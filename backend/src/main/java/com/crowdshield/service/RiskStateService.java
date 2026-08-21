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

    private volatile int holdSeconds;
    private volatile int downgradeConsecutiveFrames;
    private final Map<String, RiskState> stateByKey = new ConcurrentHashMap<>();

    public RiskStateService(CrowdShieldProperties properties) {
        this.holdSeconds = properties.getRisk().getHysteresis().getHoldSeconds();
        this.downgradeConsecutiveFrames = properties.getRisk().getHysteresis().getDowngradeConsecutiveFrames();
    }

    public synchronized RiskSnapshot apply(String key, RiskLevel computedLevel, double score, Instant now) {
        RiskState current = stateByKey.computeIfAbsent(key, k -> new RiskState(computedLevel, now, score, 0));

        RiskLevel effective = current.level();
        Instant holdUntil = current.holdUntil();
        int consecutiveLower = current.consecutiveLowerFrames();

        if (computedLevel.ordinal() > current.level().ordinal()) {
            // Escalation: immediate upgrade with hysteresis hold window
            effective = computedLevel;
            holdUntil = now.plusSeconds(holdSeconds);
            consecutiveLower = 0;
        } else if (computedLevel.ordinal() < current.level().ordinal()) {
            // Downgrade request: requires both hold expiration and sustained improvement frames
            consecutiveLower++;
            boolean holdExpired = !now.isBefore(current.holdUntil());
            boolean sustainedImprovement = consecutiveLower >= downgradeConsecutiveFrames;

            if (holdExpired && sustainedImprovement) {
                effective = computedLevel;
                holdUntil = now.plusSeconds(holdSeconds);
                consecutiveLower = 0;
            } else {
                // Sustain higher risk level to prevent flicker
                effective = current.level();
            }
        } else {
            // Level is unchanged
            effective = computedLevel;
            consecutiveLower = 0;
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

        stateByKey.put(key, new RiskState(effective, holdUntil, score, consecutiveLower));
        return new RiskSnapshot(effective, trend, holdUntil);
    }

    public void clear() {
        stateByKey.clear();
    }

    public int getHoldSeconds() {
        return holdSeconds;
    }

    public void setHoldSeconds(int holdSeconds) {
        this.holdSeconds = holdSeconds;
    }

    public int getDowngradeConsecutiveFrames() {
        return downgradeConsecutiveFrames;
    }

    public void setDowngradeConsecutiveFrames(int downgradeConsecutiveFrames) {
        this.downgradeConsecutiveFrames = downgradeConsecutiveFrames;
    }

    public record RiskSnapshot(RiskLevel level, String trend, Instant holdUntil) {}

    private record RiskState(RiskLevel level, Instant holdUntil, double lastScore, int consecutiveLowerFrames) {}
}
