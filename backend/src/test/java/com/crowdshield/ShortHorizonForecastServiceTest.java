package com.crowdshield;

import static org.assertj.core.api.Assertions.assertThat;

import com.crowdshield.model.ForecastResponse;
import com.crowdshield.model.HorizonRiskForecast;
import com.crowdshield.model.RiskLevel;
import com.crowdshield.model.ZoneForecast;
import com.crowdshield.service.ScenarioPlaybackService;
import com.crowdshield.service.ShortHorizonForecastService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ShortHorizonForecastServiceTest {

    @Autowired
    private ScenarioPlaybackService playbackService;

    @Autowired
    private ShortHorizonForecastService forecastService;

    @BeforeEach
    void setUp() {
        playbackService.load("s2_entry_bottleneck");
        playbackService.seek(40);
    }

    @Test
    void forecastIncludesAllStandardHorizonsAndDisclaimer() {
        ForecastResponse response = forecastService.getCurrentForecast();

        assertThat(response.horizonsSec()).containsExactly(30, 60, 180, 300);
        assertThat(response.disclaimer()).contains("Prototype heuristic safety scoring");
        assertThat(response.overallForecast()).isNotNull();
        assertThat(response.overallForecast().horizons()).containsKeys(30, 60, 180, 300);

        for (ZoneForecast zf : response.zoneForecasts()) {
            assertThat(zf.zoneId()).isNotEmpty();
            assertThat(zf.method()).isEqualTo("ENSEMBLE_CONSERVATIVE_BASELINE");
            assertThat(zf.confidence()).isBetween(0.0, 1.0);
            assertThat(zf.horizons()).containsKeys(30, 60, 180, 300);

            for (HorizonRiskForecast hrf : zf.horizons().values()) {
                assertThat(hrf.score()).isBetween(0.0, 1.0);
                assertThat(hrf.level()).isIn(RiskLevel.LOW, RiskLevel.MODERATE, RiskLevel.HIGH, RiskLevel.CRITICAL);
            }
        }
    }

    @Test
    void corridorZoneReflectsRisingTrendDuringBottleneck() {
        ForecastResponse response = forecastService.getCurrentForecast();

        ZoneForecast corridor = response.zoneForecasts().stream()
                .filter(z -> z.zoneId().equals("z-corridor-1"))
                .findFirst()
                .orElseThrow();

        assertThat(corridor.trend()).isEqualTo("RISING");
        assertThat(corridor.horizons().get(180).score()).isGreaterThanOrEqualTo(corridor.currentScore());
        assertThat(corridor.horizonEstimate()).isNotEmpty();
    }
}
