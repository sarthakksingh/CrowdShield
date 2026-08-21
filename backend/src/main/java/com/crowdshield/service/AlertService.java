package com.crowdshield.service;

import com.crowdshield.model.AlertRecord;
import com.crowdshield.model.RiskLevel;
import com.crowdshield.model.RiskResponse;
import com.crowdshield.model.ZoneRisk;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

    public synchronized List<AlertRecord> refreshAndGetAlerts(RiskResponse riskResponse) {
        Instant now = Instant.now();
        for (ZoneRisk zoneRisk : riskResponse.zoneRisks()) {
            if (zoneRisk.level().ordinal() >= RiskLevel.HIGH.ordinal()) {
                upsertAlert(zoneRisk, now);
            } else {
                clearZoneAlerts(zoneRisk.zoneId());
            }
        }

        return activeAlerts.values().stream()
                .sorted(Comparator.comparing(AlertRecord::lastUpdatedAt).reversed())
                .collect(Collectors.toCollection(ArrayList::new));
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
}
