package com.crowdshield;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ForecastEndpointsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getCurrentForecastReturns200AndValidStructure() throws Exception {
        mockMvc.perform(get("/api/forecast/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.eventId").exists())
                .andExpect(jsonPath("$.scenarioId").exists())
                .andExpect(jsonPath("$.currentOffsetSec").isNumber())
                .andExpect(jsonPath("$.horizonsSec").isArray())
                .andExpect(jsonPath("$.overallForecast").exists())
                .andExpect(jsonPath("$.overallForecast.trend").isString())
                .andExpect(jsonPath("$.overallForecast.horizons['30']").exists())
                .andExpect(jsonPath("$.overallForecast.horizons['60']").exists())
                .andExpect(jsonPath("$.overallForecast.horizons['180']").exists())
                .andExpect(jsonPath("$.overallForecast.horizons['300']").exists())
                .andExpect(jsonPath("$.zoneForecasts").isArray())
                .andExpect(jsonPath("$.disclaimer").exists());
    }
}
