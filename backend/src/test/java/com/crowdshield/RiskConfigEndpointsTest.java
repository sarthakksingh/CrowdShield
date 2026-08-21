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
class RiskConfigEndpointsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getRiskConfigReturnsCurrentThresholdsAndWeights() throws Exception {
        mockMvc.perform(get("/api/risk/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thresholds.moderate").isNumber())
                .andExpect(jsonPath("$.thresholds.high").isNumber())
                .andExpect(jsonPath("$.thresholds.critical").isNumber())
                .andExpect(jsonPath("$.weights.density_pressure").isNumber())
                .andExpect(jsonPath("$.hysteresisHoldSeconds").isNumber())
                .andExpect(jsonPath("$.persistenceRequiredFrames").isNumber())
                .andExpect(jsonPath("$.disclaimer").isString());
    }

    @Test
    void updateRiskConfigModifiesThresholdsAndWeightsAtRuntime() throws Exception {
        String updateRequest = """
                {
                  "thresholds": {
                    "moderate": 0.30,
                    "high": 0.60,
                    "critical": 0.80
                  },
                  "weights": {
                    "density_pressure": 0.30,
                    "inflow_outflow_imbalance": 0.20,
                    "movement_instability": 0.15,
                    "opposing_movement": 0.15,
                    "bottleneck_pressure": 0.15,
                    "route_availability": 0.05
                  },
                  "hysteresisHoldSeconds": 60,
                  "persistenceRequiredFrames": 3,
                  "disclaimer": "Custom updated disclaimer notice"
                }
                """;

        mockMvc.perform(post("/api/risk/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thresholds.moderate").value(0.30))
                .andExpect(jsonPath("$.thresholds.high").value(0.60))
                .andExpect(jsonPath("$.thresholds.critical").value(0.80))
                .andExpect(jsonPath("$.weights.density_pressure").value(0.30))
                .andExpect(jsonPath("$.hysteresisHoldSeconds").value(60))
                .andExpect(jsonPath("$.persistenceRequiredFrames").value(3))
                .andExpect(jsonPath("$.disclaimer").value("Custom updated disclaimer notice"));
    }

    @Test
    void updateRiskConfigWithInvalidThresholdsReturns400() throws Exception {
        String invalidRequest = """
                {
                  "thresholds": {
                    "moderate": 0.70,
                    "high": 0.50,
                    "critical": 0.90
                  },
                  "weights": {
                    "density_pressure": 0.30
                  },
                  "hysteresisHoldSeconds": 45,
                  "persistenceRequiredFrames": 2,
                  "disclaimer": "Test"
                }
                """;

        mockMvc.perform(post("/api/risk/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_RISK_CONFIG"));
    }
}
