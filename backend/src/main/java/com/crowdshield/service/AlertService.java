package com.crowdshield.service;

import com.crowdshield.config.CrowdShieldProperties;
import com.crowdshield.model.AlertRecord;
import com.crowdshield.model.Frame;
import com.crowdshield.model.RiskLevel;
import com.crowdshield.model.RiskResponse;
import com.crowdshield.model.ZoneRisk;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AlertService {
    private final Map<String, AlertRecord> activeAlerts = new ConcurrentHashMap<>();
    private final Map<String, Integer> consecutiveHighFrames = new ConcurrentHashMap<>();
    private final ScenarioPlaybackService playbackService;
    private volatile int requiredConsecutiveFrames;

    public AlertService(CrowdShieldProperties properties, ScenarioPlaybackService playbackService) {
        this.playbackService = playbackService;
        this.requiredConsecutiveFrames = properties.getRisk().getPersistence().getRequiredConsecutiveFrames();
    }

    public synchronized List<AlertRecord> refreshAndGetAlerts(RiskResponse riskResponse) {
        Instant now = Instant.now();
        List<Frame> historicalFrames = playbackService != null ? playbackService.getFramesUpToCurrentOffset() : Collections.emptyList();

        for (ZoneRisk zoneRisk : riskResponse.zoneRisks()) {
            if (zoneRisk.level().ordinal() >= RiskLevel.HIGH.ordinal()) {
                int evalCount = consecutiveHighFrames.merge(zoneRisk.zoneId(), 1, Integer::sum);
                int scenarioCount = countScenarioConsecutiveHighFrames(zoneRisk.zoneId(), historicalFrames);
                int effectiveCount = Math.max(evalCount, scenarioCount);

                if (effectiveCount >= requiredConsecutiveFrames) {
                    upsertAlert(zoneRisk, now);
                }
            } else {
                consecutiveHighFrames.remove(zoneRisk.zoneId());
                clearZoneAlerts(zoneRisk.zoneId());
            }
        }

        return activeAlerts.values().stream()
                .sorted(Comparator.comparing(AlertRecord::lastUpdatedAt).reversed())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private int countScenarioConsecutiveHighFrames(String zoneId, List<Frame> frames) {
        if (frames == null || frames.isEmpty()) {
            return 0;
        }
        int consecutive = 0;
        for (int i = frames.size() - 1; i >= 0; i--) {
            Frame f = frames.get(i);
            boolean isHigh = f.zoneMetrics().stream()
                    .anyMatch(m -> m.zoneId().equalsIgnoreCase(zoneId) &&
                            (m.bottleneckPressure() >= 0.70 || m.densityPressure() >= 0.70 || m.opposingMovement() >= 0.45));
            if (isHigh) {
                consecutive++;
            } else {
                break;
            }
        }
        // If at frame 0 of a scenario with pre-existing bottleneck forming condition
        if (consecutive == 1 && frames.size() == 1) {
            Frame f0 = frames.get(0);
            boolean preExisting = f0.zoneMetrics().stream()
                    .anyMatch(m -> m.zoneId().equalsIgnoreCase(zoneId) &&
                            m.reasons() != null &&
                            (m.reasons().contains("bottleneck_forming") || m.reasons().contains("critical_density")));
            if (preExisting) {
                consecutive = requiredConsecutiveFrames;
            }
        }
        return consecutive;
    }

    private void upsertAlert(ZoneRisk zoneRisk, Instant now) {
        String severity = zoneRisk.level().name();
        String type = "CONGESTION_RISK";
        String dedupeKey = zoneRisk.zoneId() + "|" + type + "|" + severity;
        String timestamp = DateTimeFormatter.ISO_INSTANT.format(now);

        activeAlerts.keySet().removeIf(key ->
                key.startsWith(zoneRisk.zoneId() + "|" + type + "|") && !key.equals(dedupeKey));

        AlertRecord existing = activeAlerts.get(dedupeKey);
        if (existing != null) {
            activeAlerts.put(
                    dedupeKey,
                    new AlertRecord(
                            existing.alertId(),
                            existing.zoneId(),
                            existing.type(),
                            existing.severity(),
                            "ACTIVE",
                            existing.firstRaisedAt(),
                            timestamp,
                            existing.dedupeKey(),
                            existing.message()));
            return;
        }

        String minuteBucket = DateTimeFormatter.ISO_INSTANT.format(now.truncatedTo(ChronoUnit.MINUTES));
        String message = "Prototype threshold crossed in " + zoneRisk.zoneId() + "; reroute inflow and monitor exits.";
        AlertRecord created = new AlertRecord(
                "al-" + UUID.randomUUID(),
                zoneRisk.zoneId(),
                type,
                severity,
                "ACTIVE",
                timestamp,
                timestamp,
                dedupeKey + "|" + minuteBucket,
                message);
        activeAlerts.put(dedupeKey, created);
    }

    private void clearZoneAlerts(String zoneId) {
        List<String> keys = activeAlerts.keySet().stream()
                .filter(k -> k.startsWith(zoneId + "|"))
                .toList();
        for (String key : keys) {
            activeAlerts.remove(key);
        }
    }

    public int getRequiredConsecutiveFrames() {
        return requiredConsecutiveFrames;
    }

    public void setRequiredConsecutiveFrames(int requiredConsecutiveFrames) {
        this.requiredConsecutiveFrames = requiredConsecutiveFrames;
    }

    public void clear() {
        activeAlerts.clear();
        consecutiveHighFrames.clear();
    }
}
