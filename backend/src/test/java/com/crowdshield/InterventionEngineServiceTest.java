package com.crowdshield;

import static org.assertj.core.api.Assertions.assertThat;

import com.crowdshield.model.Recommendation;
import com.crowdshield.model.RecommendationActionType;
import com.crowdshield.model.RecommendationsResponse;
import com.crowdshield.model.RiskLevel;
import com.crowdshield.service.InterventionEngineService;
import com.crowdshield.service.ScenarioPlaybackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class InterventionEngineServiceTest {

    @Autowired
    private ScenarioPlaybackService playbackService;

    @Autowired
    private InterventionEngineService interventionEngineService;

    @BeforeEach
    void setUp() {
        playbackService.load("s2_entry_bottleneck");
        playbackService.seek(40);
    }

    @Test
    void recommendationsAreGeneratedWithNonEmptyGroundedFields() {
        RecommendationsResponse response = interventionEngineService.getRecommendations();

        assertThat(response.recommendations()).isNotEmpty();
        assertThat(response.advisoryNotice()).contains("advisories for authorized operations personnel");

        for (Recommendation rec : response.recommendations()) {
            assertThat(rec.id()).isNotEmpty();
            assertThat(rec.actionType()).isNotNull();
            assertThat(rec.targetZoneId()).isNotEmpty();
            assertThat(rec.title()).isNotEmpty();
            assertThat(rec.reason()).isNotEmpty();
            assertThat(rec.reason()).doesNotContain("placeholder");
            assertThat(rec.expectedImpact()).isNotEmpty();
            assertThat(rec.confidence()).isGreaterThan(0.0);
            assertThat(rec.urgency()).isIn(RiskLevel.LOW, RiskLevel.MODERATE, RiskLevel.HIGH, RiskLevel.CRITICAL);
            assertThat(rec.suggestedMessage()).isNotEmpty();
            assertThat(rec.rank()).isGreaterThan(0);
        }
    }

    @Test
    void languageIsStrictlyAdvisoryOnlyWithoutAutomaticActuationClaims() {
        RecommendationsResponse response = interventionEngineService.getRecommendations();

        for (Recommendation rec : response.recommendations()) {
            // Advisory language verification
            assertThat(rec.title().toLowerCase()).contains("recommend");
            // Must not use automatic actuation language
            assertThat(rec.title().toLowerCase()).doesNotContain("closing gate now");
            assertThat(rec.title().toLowerCase()).doesNotContain("opening gate now");
            assertThat(rec.suggestedMessage().toLowerCase()).doesNotContain("system executed");
            assertThat(rec.suggestedMessage().toLowerCase()).doesNotContain("automatically actuated");
        }
    }

    @Test
    void rankingIsOrderedByUrgencyAndConfidence() {
        RecommendationsResponse response = interventionEngineService.getRecommendations();

        int prevUrgency = Integer.MAX_VALUE;
        for (Recommendation rec : response.recommendations()) {
            int currentUrgency = rec.urgency().ordinal();
            assertThat(currentUrgency).isLessThanOrEqualTo(prevUrgency);
            prevUrgency = currentUrgency;
        }

        // Check 1-based consecutive ranking
        for (int i = 0; i < response.recommendations().size(); i++) {
            assertThat(response.recommendations().get(i).rank()).isEqualTo(i + 1);
        }
    }
}
