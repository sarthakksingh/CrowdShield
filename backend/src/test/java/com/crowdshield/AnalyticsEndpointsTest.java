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
class AnalyticsEndpointsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getCurrentAnalyticsReturns200AndValidStructure() throws Exception {
        mockMvc.perform(get("/api/analytics/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.eventId").exists())
                .andExpect(jsonPath("$.scenarioId").exists())
                .andExpect(jsonPath("$.currentOffsetSec").isNumber())
                .andExpect(jsonPath("$.zones").isArray())
                .andExpect(jsonPath("$.zones[0].zoneId").exists())
                .andExpect(jsonPath("$.zones[0].netPressure").isNumber())
                .andExpect(jsonPath("$.zones[0].speedDropPct").isNumber())
                .andExpect(jsonPath("$.zones[0].densityTrend").isString())
                .andExpect(jsonPath("$.zones[0].inflowTrend").isString())
                .andExpect(jsonPath("$.zones[0].outflowTrend").isString())
                .andExpect(jsonPath("$.zones[0].queueTrend").isString())
                .andExpect(jsonPath("$.zones[0].bottleneck").exists())
                .andExpect(jsonPath("$.zones[0].counterflow").exists())
                .andExpect(jsonPath("$.summary").exists());
    }

    @Test
    void getZoneAnalyticsReturns200AndTimeline() throws Exception {
        mockMvc.perform(get("/api/analytics/zones/z-corridor-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.eventId").exists())
                .andExpect(jsonPath("$.scenarioId").exists())
                .andExpect(jsonPath("$.analytics.zoneId").value("z-corridor-1"))
                .andExpect(jsonPath("$.analytics.bottleneck.reasons").isArray())
                .andExpect(jsonPath("$.timeline").isArray());
    }

    @Test
    void getZoneAnalyticsForInvalidZoneReturns404() throws Exception {
        mockMvc.perform(get("/api/analytics/zones/non-existent-zone"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ZONE_NOT_FOUND"));
    }

    @Test
    void analyticsValuesChangeWhenPlaybackIsSeeked() throws Exception {
        // Load bottleneck scenario and seek to 0
        mockMvc.perform(post("/api/playback/load")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scenarioId": "s2_entry_bottleneck"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/playback/seek")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"offsetSec": 0}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/analytics/zones/z-corridor-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentOffsetSec").value(0))
                .andExpect(jsonPath("$.analytics.densityPerSqM").value(3.2));

        // Seek to offset 40
        mockMvc.perform(post("/api/playback/seek")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"offsetSec": 40}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/analytics/zones/z-corridor-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentOffsetSec").value(40))
                .andExpect(jsonPath("$.analytics.densityPerSqM").value(4.1))
                .andExpect(jsonPath("$.analytics.densityTrend").value("RISING"));
    }
}
