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
class RecommendationEndpointsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getCurrentRecommendationsReturns200AndValidStructure() throws Exception {
        mockMvc.perform(get("/api/recommendations/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.eventId").exists())
                .andExpect(jsonPath("$.scenarioId").exists())
                .andExpect(jsonPath("$.currentOffsetSec").isNumber())
                .andExpect(jsonPath("$.overallRiskLevel").exists())
                .andExpect(jsonPath("$.recommendations").isArray())
                .andExpect(jsonPath("$.advisoryNotice").isString())
                .andExpect(jsonPath("$.summary").exists());
    }

    @Test
    void recommendationsEvolveWhenPlaybackIsSeeked() throws Exception {
        // Load normal flow
        mockMvc.perform(post("/api/playback/load")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scenarioId": "s1_normal_flow"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/playback/seek")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"offsetSec": 0}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/recommendations/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentOffsetSec").value(0))
                .andExpect(jsonPath("$.summary.criticalActionCount").value(0));

        // Load entry bottleneck at offset 40
        mockMvc.perform(post("/api/playback/load")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scenarioId": "s2_entry_bottleneck"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/playback/seek")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"offsetSec": 40}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/recommendations/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentOffsetSec").value(40))
                .andExpect(jsonPath("$.recommendations").isNotEmpty())
                .andExpect(jsonPath("$.recommendations[0].urgency").value("CRITICAL"));
    }
}
