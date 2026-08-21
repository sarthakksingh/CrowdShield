package com.crowdshield;

import static org.assertj.core.api.Assertions.assertThat;

import com.crowdshield.model.Recommendation;
import com.crowdshield.model.RecommendationActionType;
import com.crowdshield.model.RecommendationsResponse;
import com.crowdshield.model.RiskLevel;
import com.crowdshield.service.InterventionEngineService;
import com.crowdshield.service.ScenarioPlaybackService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RecommendationScenariosTest {

    @Autowired
    private ScenarioPlaybackService playbackService;

    @Autowired
    private InterventionEngineService interventionEngineService;

    @Test
    void scenario1NormalFlowProducesNoCriticalOrHighRecommendations() {
        playbackService.load("s1_normal_flow");
        playbackService.seek(0);

        RecommendationsResponse response = interventionEngineService.getRecommendations();
        assertThat(response.scenarioId()).isEqualTo("s1_normal_flow");

        // No critical or high urgent actions in normal flow
        long urgentCount = response.recommendations().stream()
                .filter(r -> r.urgency() == RiskLevel.HIGH || r.urgency() == RiskLevel.CRITICAL)
                .count();
        assertThat(urgentCount).isZero();
    }

    @Test
    void scenario2EntryBottleneckGeneratesActionableInterventions() {
        playbackService.load("s2_entry_bottleneck");
        playbackService.seek(40);

        RecommendationsResponse response = interventionEngineService.getRecommendations();
        assertThat(response.recommendations()).isNotEmpty();

        // Must recommend gate restriction on upstream entry
        assertThat(response.recommendations()).anyMatch(r ->
                r.actionType() == RecommendationActionType.RESTRICT_GATE &&
                r.urgency() == RiskLevel.CRITICAL &&
                r.reason().contains("z-corridor-1"));

        // Must recommend opening route bypass
        assertThat(response.recommendations()).anyMatch(r ->
                r.actionType() == RecommendationActionType.OPEN_ROUTE &&
                r.targetZoneId().equals("z-corridor-1"));

        // Must recommend deploying safety personnel
        assertThat(response.recommendations()).anyMatch(r ->
                r.actionType() == RecommendationActionType.DEPLOY_PERSONNEL &&
                r.targetZoneId().equals("z-corridor-1"));

        // Must recommend broadcast alert
        assertThat(response.recommendations()).anyMatch(r ->
                r.actionType() == RecommendationActionType.BROADCAST_ALERT);
    }

    @Test
    void scenario3CounterflowPanicRecommendsFlowRedirectionAndCalmingBroadcast() {
        playbackService.load("s3_counterflow_panic");
        playbackService.seek(0);

        RecommendationsResponse response = interventionEngineService.getRecommendations();
        assertThat(response.recommendations()).isNotEmpty();

        // Recommends flow redirection
        assertThat(response.recommendations()).anyMatch(r ->
                r.actionType() == RecommendationActionType.REDIRECT_INFLOW &&
                r.urgency() == RiskLevel.CRITICAL);

        // Recommends public safety broadcast with calming tone
        Recommendation broadcastRec = response.recommendations().stream()
                .filter(r -> r.actionType() == RecommendationActionType.BROADCAST_ALERT)
                .findFirst()
                .orElseThrow();
        assertThat(broadcastRec.urgency()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(broadcastRec.suggestedMessage()).contains("remain calm");
    }

    @Test
    void scenario4PostInterventionRecoveryReflectsDecreasingInterventionUrgency() {
        playbackService.load("s4_post_intervention_recovery");

        // Offset 0 (still congested prior to full dispersal)
        playbackService.seek(0);
        RecommendationsResponse initial = interventionEngineService.getRecommendations();

        // Offset 100 (intervention has relieved pressure)
        playbackService.seek(100);
        RecommendationsResponse recovered = interventionEngineService.getRecommendations();

        long initialCritical = initial.recommendations().stream()
                .filter(r -> r.urgency() == RiskLevel.CRITICAL)
                .count();
        long recoveredCritical = recovered.recommendations().stream()
                .filter(r -> r.urgency() == RiskLevel.CRITICAL)
                .count();

        // Critical recommendations decrease significantly or disappear
        assertThat(recoveredCritical).isLessThan(initialCritical);
    }
}
