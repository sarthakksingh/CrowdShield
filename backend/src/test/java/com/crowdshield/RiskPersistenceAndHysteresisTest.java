package com.crowdshield;

import static org.assertj.core.api.Assertions.assertThat;

import com.crowdshield.model.AlertRecord;
import com.crowdshield.model.OverallRisk;
import com.crowdshield.model.RiskLevel;
import com.crowdshield.model.RiskResponse;
import com.crowdshield.model.ZoneRisk;
import com.crowdshield.service.AlertService;
import com.crowdshield.service.RiskScoringService;
import com.crowdshield.service.RiskStateService;
import com.crowdshield.service.ScenarioPlaybackService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RiskPersistenceAndHysteresisTest {

    @Autowired
    private RiskScoringService riskScoringService;

    @Autowired
    private RiskStateService riskStateService;

    @Autowired
    private AlertService alertService;

    @Autowired
    private ScenarioPlaybackService playbackService;

    @BeforeEach
    void resetState() {
        playbackService.load("s1_normal_flow");
        playbackService.seek(0);
        alertService.clear();
        riskStateService.clear();
        alertService.setRequiredConsecutiveFrames(2);
    }

    @Test
    void singleAnomalousBadFrameDoesNotTriggerCriticalAlertOnItsOwn() {
        // Frame 1: Low risk
        ZoneRisk lowRisk = new ZoneRisk(
                "z-corridor-1", 0.20, RiskLevel.LOW, "STABLE", 0.90, List.of(), Map.of(), "2026-08-22T00:00:00Z");
        RiskResponse response1 = new RiskResponse(
                new OverallRisk(0.20, RiskLevel.LOW, "STABLE", 0.90, List.of(), Map.of(), "2026-08-22T00:00:00Z"),
                List.of(lowRisk));
        List<AlertRecord> alerts1 = alertService.refreshAndGetAlerts(response1);
        assertThat(alerts1).isEmpty();

        // Frame 2: Single anomalous spike to CRITICAL
        ZoneRisk spikeRisk = new ZoneRisk(
                "z-corridor-1", 0.88, RiskLevel.CRITICAL, "RISING", 0.90, List.of(), Map.of(), "2026-08-22T00:00:00Z");
        RiskResponse response2 = new RiskResponse(
                new OverallRisk(0.88, RiskLevel.CRITICAL, "RISING", 0.90, List.of(), Map.of(), "2026-08-22T00:00:00Z"),
                List.of(spikeRisk));
        List<AlertRecord> alertsAfterSpike = alertService.refreshAndGetAlerts(response2);

        // Verification: Persistence filter blocked single-frame spike
        assertThat(alertsAfterSpike).isEmpty();

        // Frame 3: Returned to normal
        List<AlertRecord> alertsAfterReturn = alertService.refreshAndGetAlerts(response1);
        assertThat(alertsAfterReturn).isEmpty();
    }

    @Test
    void sustainedHighRiskForConsecutiveFramesTriggersAlert() {
        ZoneRisk highRisk = new ZoneRisk(
                "z-corridor-1", 0.78, RiskLevel.CRITICAL, "RISING", 0.90, List.of(), Map.of(), "2026-08-22T00:00:00Z");
        RiskResponse highResponse = new RiskResponse(
                new OverallRisk(0.78, RiskLevel.CRITICAL, "RISING", 0.90, List.of(), Map.of(), "2026-08-22T00:00:00Z"),
                List.of(highRisk));

        // 1st frame: suppressed by persistence requirement
        List<AlertRecord> alerts1 = alertService.refreshAndGetAlerts(highResponse);
        assertThat(alerts1).isEmpty();

        // 2nd frame: sustained -> fires alert!
        List<AlertRecord> alerts2 = alertService.refreshAndGetAlerts(highResponse);
        assertThat(alerts2).hasSize(1);
        assertThat(alerts2.get(0).severity()).isEqualTo("CRITICAL");
        assertThat(alerts2.get(0).zoneId()).isEqualTo("z-corridor-1");
    }

    @Test
    void rapidFlickerBetweenHighAndCriticalDoesNotSpamAlerts() {
        ZoneRisk highRisk = new ZoneRisk(
                "z-corridor-1", 0.65, RiskLevel.HIGH, "RISING", 0.90, List.of(), Map.of(), "2026-08-22T00:00:00Z");
        ZoneRisk critRisk = new ZoneRisk(
                "z-corridor-1", 0.85, RiskLevel.CRITICAL, "RISING", 0.90, List.of(), Map.of(), "2026-08-22T00:00:00Z");

        // Establish high alert
        alertService.refreshAndGetAlerts(new RiskResponse(null, List.of(highRisk)));
        List<AlertRecord> alerts = alertService.refreshAndGetAlerts(new RiskResponse(null, List.of(highRisk)));
        assertThat(alerts).hasSize(1);

        // Flicker to CRITICAL
        List<AlertRecord> critAlerts = alertService.refreshAndGetAlerts(new RiskResponse(null, List.of(critRisk)));
        assertThat(critAlerts).hasSize(1);
        assertThat(critAlerts.get(0).severity()).isEqualTo("CRITICAL");

        // Flicker repeatedly
        alertService.refreshAndGetAlerts(new RiskResponse(null, List.of(critRisk)));
        List<AlertRecord> stableAlerts = alertService.refreshAndGetAlerts(new RiskResponse(null, List.of(critRisk)));
        assertThat(stableAlerts).hasSize(1);
    }

    @Test
    void riskStateServiceRequiresSustainedImprovementToDowngrade() {
        Instant now = Instant.now();
        String zoneKey = "z-corridor-1";

        // Escalate to CRITICAL
        RiskStateService.RiskSnapshot s1 = riskStateService.apply(zoneKey, RiskLevel.CRITICAL, 0.85, now);
        assertThat(s1.level()).isEqualTo(RiskLevel.CRITICAL);

        // 1 frame of LOW after hold expired: should NOT downgrade immediately (requires 2 consecutive improvement frames)
        Instant future = now.plusSeconds(100);
        RiskStateService.RiskSnapshot s2 = riskStateService.apply(zoneKey, RiskLevel.LOW, 0.20, future);
        assertThat(s2.level()).isEqualTo(RiskLevel.CRITICAL);

        // 2nd consecutive frame of LOW: sustained improvement -> successfully downgraded
        RiskStateService.RiskSnapshot s3 = riskStateService.apply(zoneKey, RiskLevel.LOW, 0.20, future.plusSeconds(5));
        assertThat(s3.level()).isEqualTo(RiskLevel.LOW);
    }

    @Test
    void riskResponseIncludesFactorDescriptionsHorizonAndDisclaimer() {
        playbackService.load("s2_entry_bottleneck");
        playbackService.seek(40);

        RiskResponse risk = riskScoringService.calculateCurrentRisk();

        // Safety disclaimer present
        assertThat(risk.disclaimer()).contains("Prototype heuristic safety scoring");
        assertThat(risk.overallRisk().disclaimer()).contains("Prototype heuristic safety scoring");

        // Horizon estimate present
        assertThat(risk.overallRisk().horizon()).isNotEmpty();

        // Factor descriptions present
        assertThat(risk.overallRisk().factorDescriptions()).isNotEmpty();
        assertThat(risk.overallRisk().factorDescriptions()).containsKey("density_pressure");
        assertThat(risk.overallRisk().factorDescriptions().get("density_pressure")).contains("Density pressure contributed");

        ZoneRisk corridor = risk.zoneRisks().stream()
                .filter(z -> z.zoneId().equals("z-corridor-1"))
                .findFirst()
                .orElseThrow();

        assertThat(corridor.factorDescriptions()).isNotEmpty();
        assertThat(corridor.horizon()).isIn("immediate (< 1 min)", "1-2 min", "2-4 min");
    }
}
