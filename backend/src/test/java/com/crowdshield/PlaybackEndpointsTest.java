package com.crowdshield;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PlaybackEndpointsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getPlaybackReturnsCurrentState() throws Exception {
        mockMvc.perform(get("/api/playback"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scenarioId").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.currentOffsetSec").isNumber());
    }

    @Test
    void getScenariosReturnsAvailableScenarios() throws Exception {
        mockMvc.perform(get("/api/scenarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scenarios").isArray())
                .andExpect(jsonPath("$.scenarios[0].scenarioId").exists());
    }

    @Test
    void loadScenarioChangesCurrentScenario() throws Exception {
        String loadRequest = """
                {"scenarioId": "s1_normal_flow"}
                """;
        mockMvc.perform(post("/api/playback/load")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loadRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scenarioId").value("s1_normal_flow"));
    }

    @Test
    void playStartsPlayback() throws Exception {
        String playRequest = """
                {"speed": 1.0}
                """;
        mockMvc.perform(post("/api/playback/play")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(playRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PLAYING"));
    }

    @Test
    void pauseStopsPlayback() throws Exception {
        mockMvc.perform(post("/api/playback/pause"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAUSED"));
    }

    @Test
    void resetMovesToBeginning() throws Exception {
        mockMvc.perform(post("/api/playback/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentOffsetSec").value(0));
    }

    @Test
    void seekChangesOffset() throws Exception {
        String seekRequest = """
                {"offsetSec": 50}
                """;
        mockMvc.perform(post("/api/playback/seek")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(seekRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentOffsetSec").value(50));
    }

    @Test
    void invalidPlaybackRequestReturns400() throws Exception {
        String invalidRequest = """
                {"scenarioId": ""}
                """;
        mockMvc.perform(post("/api/playback/load")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_PLAYBACK_REQUEST"));
    }

    @Test
    void invalidSimulationRequestReturnsCorrectError() throws Exception {
        String invalidSimulation = """
                {
                  "eventId": "event-tech-nova-2026",
                  "scenarioName": "test",
                  "actions": [],
                  "horizonSec": 0
                }
                """;
        mockMvc.perform(post("/api/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidSimulation))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_SIMULATION_REQUEST"));
    }
}
