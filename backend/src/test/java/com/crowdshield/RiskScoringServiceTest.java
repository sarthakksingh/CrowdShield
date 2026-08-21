package com.crowdshield;

import static org.assertj.core.api.Assertions.assertThat;

import com.crowdshield.model.RiskLevel;
import com.crowdshield.model.RiskResponse;
import com.crowdshield.service.RiskScoringService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RiskScoringServiceTest {

    @Autowired
    private RiskScoringService riskScoringService;

    @Test
    void bottleneckScenarioProducesHighOrCriticalRisk() {
        RiskResponse risk = riskScoringService.calculateCurrentRisk();
        assertThat(risk.overallRisk().level()).isIn(RiskLevel.HIGH, RiskLevel.CRITICAL);
        assertThat(risk.overallRisk().factorContributions()).containsKeys(
                "density_pressure",
                "inflow_outflow_imbalance",
                "movement_instability",
                "opposing_movement",
                "bottleneck_pressure",
                "route_availability");
    }
}
