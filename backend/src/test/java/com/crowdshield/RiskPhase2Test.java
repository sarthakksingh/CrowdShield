package com.crowdshield;

import static org.assertj.core.api.Assertions.assertThat;

import com.crowdshield.model.RiskLevel;
import com.crowdshield.model.RiskResponse;
import com.crowdshield.service.RiskScoringService;
import com.crowdshield.service.ScenarioPlaybackService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RiskPhase2Test {

    @Autowired
    private ScenarioPlaybackService playbackService;

    @Autowired
    private RiskScoringService riskScoringService;

    @Test
    void riskChangesBetweenEarlyAndLateBottleneckFrames() {
        playbackService.load("s2_entry_bottleneck");
        playbackService.seek(0);
        RiskResponse early = riskScoringService.calculateCurrentRisk();

        playbackService.seek(40);
        RiskResponse late = riskScoringService.calculateCurrentRisk();

        assertThat(early.overallRisk().score()).isLessThan(late.overallRisk().score());
    }

    @Test
    void bottleneckScenarioReachesHighOrCritical() {
        playbackService.load("s2_entry_bottleneck");
        playbackService.seek(40);
        RiskResponse risk = riskScoringService.calculateCurrentRisk();
        assertThat(risk.overallRisk().level()).isIn(RiskLevel.HIGH, RiskLevel.CRITICAL);
    }

    @Test
    void recoveryScenarioHasLowerFinalRiskThanBottleneckPeak() {
        playbackService.load("s2_entry_bottleneck");
        playbackService.seek(40);
        RiskResponse bottleneckPeak = riskScoringService.calculateCurrentRisk();

        playbackService.load("s4_post_intervention_recovery");
        playbackService.seek(75);
        RiskResponse recoveryEnd = riskScoringService.calculateCurrentRisk();

        assertThat(recoveryEnd.overallRisk().score()).isLessThan(bottleneckPeak.overallRisk().score());
    }
}
