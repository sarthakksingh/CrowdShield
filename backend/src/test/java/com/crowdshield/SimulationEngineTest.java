package com.crowdshield;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.crowdshield.model.PlaybackState;
import com.crowdshield.model.RiskResponse;
import com.crowdshield.service.RiskScoringService;
import com.crowdshield.service.ScenarioPlaybackService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SimulationEngineTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ScenarioPlaybackService playbackService;

    @Autowired
    private RiskScoringService riskScoringService;

    @Test
    void simulationMeasurablyLowersProjectedRiskForBottleneckScenario() throws Exception {
        playbackService.load("s2_entry_bottleneck");
        playbackService.seek(40);

        String simRequest = """
                {
                  "eventId": "event-tech-nova-2026",
                  "scenarioName": "meter-entry-open-bypass",
                  "actions": [
                    {"type":"RESTRICT_GATE","targetId":"z-entry-a","atOffsetSec":0},
                    {"type":"OPEN_ROUTE","targetId":"z-corridor-1","atOffsetSec":10}
                  ],
                  "horizonSec": 300
                }
                """;

        mockMvc.perform(post("/api/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(simRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.heuristic").value(true))
                .andExpect(jsonPath("$.disclaimer").exists())
                .andExpect(jsonPath("$.verdict").value("IMPROVED"))
                .andExpect(jsonPath("$.baseline.peakRisk").isNumber())
                .andExpect(jsonPath("$.projected.peakRisk").isNumber())
                .andExpect(jsonPath("$.delta.peakRisk").isNumber())
                .andExpect(jsonPath("$.zoneImpacts").isArray())
                .andExpect(jsonPath("$.riskTransferNotes").isArray())
                .andExpect(jsonPath("$.recommendation").isString());
    }

    @Test
    void simulationFlagsTransferredRiskWhenInflowIsRestricted() throws Exception {
        playbackService.load("s2_entry_bottleneck");
        playbackService.seek(40);

        String simRequest = """
                {
                  "eventId": "event-tech-nova-2026",
                  "scenarioName": "close-gate-b",
                  "actions": [
                    {"type":"CLOSE_GATE","targetId":"z-entry-a","atOffsetSec":0}
                  ],
                  "horizonSec": 180
                }
                """;

        mockMvc.perform(post("/api/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(simRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskTransferNotes").isNotEmpty())
                .andExpect(jsonPath("$.riskTransferNotes[0]").value(org.hamcrest.Matchers.containsString("z-entry-a")));
    }

    @Test
    void simulationDoesNotMutateLivePlaybackOrRiskState() throws Exception {
        playbackService.load("s2_entry_bottleneck");
        playbackService.seek(40);

        PlaybackState stateBefore = playbackService.getState();
        RiskResponse riskBefore = riskScoringService.calculateCurrentRisk();

        String simRequest = """
                {
                  "eventId": "event-tech-nova-2026",
                  "scenarioName": "large-scale-intervention",
                  "actions": [
                    {"type":"RESTRICT_GATE","targetId":"z-entry-a","atOffsetSec":0},
                    {"type":"OPEN_ROUTE","targetId":"z-corridor-1","atOffsetSec":0},
                    {"type":"DEPLOY_PERSONNEL","targetId":"z-corridor-1","atOffsetSec":0}
                  ],
                  "horizonSec": 300
                }
                """;

        mockMvc.perform(post("/api/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(simRequest))
                .andExpect(status().isOk());

        PlaybackState stateAfter = playbackService.getState();
        RiskResponse riskAfter = riskScoringService.calculateCurrentRisk();

        // Verification: Live state is untouched
        assertThat(stateAfter.scenarioId()).isEqualTo(stateBefore.scenarioId());
        assertThat(stateAfter.currentOffsetSec()).isEqualTo(stateBefore.currentOffsetSec());
        assertThat(riskAfter.overallRisk().score()).isEqualTo(riskBefore.overallRisk().score());
        assertThat(riskAfter.zoneRisks()).hasSameSizeAs(riskBefore.zoneRisks());
    }

    @Test
    void counterflowPanicSimulationWithRedirectionLowersRisk() throws Exception {
        playbackService.load("s3_counterflow_panic");
        playbackService.seek(0);

        String simRequest = """
                {
                  "eventId": "event-tech-nova-2026",
                  "scenarioName": "redirect-counterflow",
                  "actions": [
                    {"type":"REDIRECT_INFLOW","targetId":"z-corridor-1","atOffsetSec":0},
                    {"type":"DEPLOY_PERSONNEL","targetId":"z-corridor-1","atOffsetSec":0}
                  ],
                  "horizonSec": 240
                }
                """;

        mockMvc.perform(post("/api/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(simRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("IMPROVED"));
    }
}
