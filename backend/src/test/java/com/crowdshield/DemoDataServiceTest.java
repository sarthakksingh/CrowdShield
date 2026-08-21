package com.crowdshield;

import static org.assertj.core.api.Assertions.assertThat;

import com.crowdshield.service.DemoDataService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DemoDataServiceTest {

    @Autowired
    private DemoDataService demoDataService;

    @Test
    void venueJsonLoadsSuccessfully() {
        assertThat(demoDataService.getVenueGraph()).isNotNull();
        assertThat(demoDataService.getVenueGraph().zones()).isNotEmpty();
        assertThat(demoDataService.getVenueGraph().edges()).isNotEmpty();
    }

    @Test
    void scenarioJsonLoadsSuccessfully() {
        assertThat(demoDataService.getScenarioData()).isNotNull();
        assertThat(demoDataService.getScenarioData().zoneMetrics()).isNotEmpty();
        assertThat(demoDataService.getScenarioData().eventId()).isEqualTo("event-tech-nova-2026");
    }
}
