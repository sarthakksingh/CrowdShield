package com.crowdshield;

import static org.assertj.core.api.Assertions.assertThat;

import com.crowdshield.model.PlaybackState;
import com.crowdshield.model.Scenario;
import com.crowdshield.service.ScenarioPlaybackService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ScenarioPlaybackServiceTest {

    @Autowired
    private ScenarioPlaybackService playbackService;

    @Test
    void allScenariosLoadSuccessfully() {
        List<Scenario> scenarios = playbackService.listScenarios();
        assertThat(scenarios).isNotEmpty();
        assertThat(scenarios).hasSize(4);
        assertThat(scenarios.stream().map(Scenario::scenarioId))
                .containsExactlyInAnyOrder("s1_normal_flow", "s2_entry_bottleneck", "s3_counterflow_panic", "s4_post_intervention_recovery");
    }

    @Test
    void playbackLoadSelectsRequestedScenario() {
        playbackService.load("s1_normal_flow");
        PlaybackState state = playbackService.getState();
        assertThat(state.scenarioId()).isEqualTo("s1_normal_flow");
    }

    @Test
    void playbackSeekChangesCurrentFrame() {
        playbackService.load("s2_entry_bottleneck");
        playbackService.seek(40);
        PlaybackState state = playbackService.getState();
        assertThat(state.currentOffsetSec()).isGreaterThanOrEqualTo(40);
    }

    @Test
    void zonesReflectCurrentFrameAfterSeek() {
        playbackService.load("s2_entry_bottleneck");
        playbackService.seek(0);
        PlaybackState state1 = playbackService.getState();
        double density1 = state1.currentZoneMetrics().stream()
                .filter(z -> z.zoneId().equals("z-corridor-1"))
                .map(z -> z.densityPressure())
                .findFirst()
                .orElse(0.0);

        playbackService.seek(40);
        PlaybackState state2 = playbackService.getState();
        double density2 = state2.currentZoneMetrics().stream()
                .filter(z -> z.zoneId().equals("z-corridor-1"))
                .map(z -> z.densityPressure())
                .findFirst()
                .orElse(0.0);

        assertThat(density1).isNotEqualTo(density2);
    }
}
