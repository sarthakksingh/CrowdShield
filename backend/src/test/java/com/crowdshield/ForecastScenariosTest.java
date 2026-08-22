package com.crowdshield;

import static org.assertj.core.api.Assertions.assertThat;

import com.crowdshield.model.ForecastResponse;
import com.crowdshield.model.RiskLevel;
import com.crowdshield.model.ZoneForecast;
import com.crowdshield.service.ScenarioPlaybackService;
import com.crowdshield.service.ShortHorizonForecastService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ForecastScenariosTest {

    @Autowired
    private ScenarioPlaybackService playbackService;

    @Autowired
    private ShortHorizonForecastService forecastService;

    @Test
    void scenario1NormalFlowForecastShowsStableLowRisk() {
        playbackService.load("s1_normal_flow");
        playbackService.seek(0);

        ForecastResponse forecast = forecastService.getCurrentForecast();

        assertThat(forecast.overallForecast().trend()).isEqualTo("STABLE");
        assertThat(forecast.overallForecast().currentLevel()).isEqualTo(RiskLevel.LOW);

        for (ZoneForecast zf : forecast.zoneForecasts()) {
            assertThat(zf.trend()).isEqualTo("STABLE");
            assertThat(zf.currentLevel()).isEqualTo(RiskLevel.LOW);
        }
    }

    @Test
    void scenario2EntryBottleneckForecastShowsRisingTrendDuringBuildup() {
        playbackService.load("s2_entry_bottleneck");
        playbackService.seek(40);

        ForecastResponse forecast = forecastService.getCurrentForecast();

        // Overall trend during buildup is RISING
        assertThat(forecast.overallForecast().trend()).isEqualTo("RISING");

        ZoneForecast corridor = forecast.zoneForecasts().stream()
                .filter(z -> z.zoneId().equals("z-corridor-1"))
                .findFirst()
                .orElseThrow();

        assertThat(corridor.trend()).isEqualTo("RISING");
        assertThat(corridor.horizons().get(60).score()).isGreaterThanOrEqualTo(corridor.currentScore());
    }

    @Test
    void scenario4PostInterventionRecoveryForecastShowsFallingTrend() {
        playbackService.load("s4_post_intervention_recovery");
        // Seek to mid-recovery (e.g. 50-60s) where historical frames show decompression
        playbackService.seek(60);

        ForecastResponse forecast = forecastService.getCurrentForecast();

        ZoneForecast corridor = forecast.zoneForecasts().stream()
                .filter(z -> z.zoneId().equals("z-corridor-1"))
                .findFirst()
                .orElseThrow();

        assertThat(corridor.trend()).isEqualTo("FALLING");
        assertThat(corridor.horizons().get(60).score()).isLessThanOrEqualTo(corridor.currentScore());
    }
}
