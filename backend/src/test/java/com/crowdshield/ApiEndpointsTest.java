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
class ApiEndpointsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void requiredEndpointsReturnSuccessForSampleData() throws Exception {
        mockMvc.perform(get("/api/events/current")).andExpect(status().isOk());
        mockMvc.perform(get("/api/venue")).andExpect(status().isOk());
        mockMvc.perform(get("/api/zones")).andExpect(status().isOk());
        mockMvc.perform(get("/api/risk/current")).andExpect(status().isOk());
        mockMvc.perform(get("/api/alerts")).andExpect(status().isOk());
        mockMvc.perform(get("/api/routes/safest").param("fromZoneId", "z-corridor-1")).andExpect(status().isOk());
        mockMvc.perform(get("/api/stream")).andExpect(status().isOk());

        String simulationRequest = """
                {
                  "eventId": "event-tech-nova-2026",
                  "scenarioName": "close-gate-b-open-corridor-c",
                  "actions": [
                    {"type":"CLOSE_GATE","targetId":"gate-b","atOffsetSec":0},
                    {"type":"OPEN_ROUTE","targetId":"corridor-c","atOffsetSec":20}
                  ],
                  "horizonSec": 300
                }
                """;
        mockMvc.perform(post("/api/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(simulationRequest))
                .andExpect(status().isOk());
    }

    @Test
    void alertsEndpointReturnsAtLeastOneMeaningfulAlert() throws Exception {
        mockMvc.perform(get("/api/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alerts[0].type").value("CONGESTION_RISK"));
    }

    @Test
    void invalidIncidentReportReturns400() throws Exception {
        String invalidIncident = """
                {
                  "eventId": "event-tech-nova-2026",
                  "reporterType": "CITIZEN",
                  "location": {"lat": 28.1022, "lon": 77.2011, "zoneId": "z-corridor-1"},
                  "category": "",
                  "severity": "MEDIUM",
                  "description": "report",
                  "reportedAt": "2026-08-13T17:39:20Z"
                }
                """;
        mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidIncident))
                .andExpect(status().isBadRequest());
    }
}
