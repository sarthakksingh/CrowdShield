package com.crowdshield;

import static org.assertj.core.api.Assertions.assertThat;

import com.crowdshield.model.AlertRecord;
import com.crowdshield.model.RiskResponse;
import com.crowdshield.service.AlertService;
import com.crowdshield.service.RiskScoringService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AlertServiceTest {

    @Autowired
    private AlertService alertService;

    @Autowired
    private RiskScoringService riskScoringService;

    @Test
    void duplicateAlertIsNotRepeatedlyCreated() {
        RiskResponse risk = riskScoringService.calculateCurrentRisk();
        List<AlertRecord> first = alertService.refreshAndGetAlerts(risk);
        List<AlertRecord> second = alertService.refreshAndGetAlerts(risk);

        assertThat(first).isNotEmpty();
        assertThat(second).hasSameSizeAs(first);
        assertThat(second.stream().map(AlertRecord::dedupeKey).distinct().count()).isEqualTo(second.size());
    }
}
