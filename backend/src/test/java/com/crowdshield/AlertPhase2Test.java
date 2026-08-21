package com.crowdshield;

import static org.assertj.core.api.Assertions.assertThat;

import com.crowdshield.model.AlertRecord;
import com.crowdshield.model.RiskResponse;
import com.crowdshield.service.AlertService;
import com.crowdshield.service.RiskScoringService;
import com.crowdshield.service.ScenarioPlaybackService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AlertPhase2Test {

    @Autowired
    private AlertService alertService;

    @Autowired
    private RiskScoringService riskScoringService;

    @Autowired
    private ScenarioPlaybackService playbackService;

    @Test
    void alertsDedupAcrossRepeatedRefreshes() {
        playbackService.load("s2_entry_bottleneck");
        playbackService.seek(40);
        
        RiskResponse risk1 = riskScoringService.calculateCurrentRisk();
        List<AlertRecord> alerts1 = alertService.refreshAndGetAlerts(risk1);

        RiskResponse risk2 = riskScoringService.calculateCurrentRisk();
        List<AlertRecord> alerts2 = alertService.refreshAndGetAlerts(risk2);

        assertThat(alerts1).isNotEmpty();
        assertThat(alerts2).hasSameSizeAs(alerts1);
        
        List<String> dedupeKeys1 = alerts1.stream().map(AlertRecord::dedupeKey).toList();
        List<String> dedupeKeys2 = alerts2.stream().map(AlertRecord::dedupeKey).toList();
        assertThat(dedupeKeys2).containsAll(dedupeKeys1);
    }

    @Test
    void alertsReflectTimelineAwareness() {
        playbackService.load("s2_entry_bottleneck");
        
        // Early frame - lower risk
        playbackService.seek(0);
        RiskResponse earlyRisk = riskScoringService.calculateCurrentRisk();
        List<AlertRecord> earlyAlerts = alertService.refreshAndGetAlerts(earlyRisk);
        
        // Later frame - higher risk
        playbackService.seek(40);
        RiskResponse lateRisk = riskScoringService.calculateCurrentRisk();
        List<AlertRecord> lateAlerts = alertService.refreshAndGetAlerts(lateRisk);
        
        // Later frame should have alerts for higher risk zones
        assertThat(lateAlerts.stream()
                .filter(a -> a.severity().equals("CRITICAL"))
                .count()).isGreaterThan(
                        earlyAlerts.stream()
                                .filter(a -> a.severity().equals("CRITICAL"))
                                .count()
        );
    }
}
