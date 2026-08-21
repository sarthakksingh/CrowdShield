package com.crowdshield.service;

import com.crowdshield.config.CrowdShieldProperties;
import com.crowdshield.model.Frame;
import com.crowdshield.model.Scenario;
import com.crowdshield.model.ScenarioData;
import com.crowdshield.model.VenueGraph;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.stereotype.Service;

@Service
public class DemoDataService {
    private final ObjectMapper objectMapper;
    private final CrowdShieldProperties properties;
    private VenueGraph venueGraph;
    private ScenarioData scenarioData;

    public DemoDataService(ObjectMapper objectMapper, CrowdShieldProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @PostConstruct
    public void load() {
        try {
            Path base = Paths.get(properties.getDemoDataDir()).toAbsolutePath().normalize();
            Path venuePath = base.resolve("venue").resolve("venue_graph.json");
            Path scenarioPath = base.resolve("scenarios").resolve("s2_entry_bottleneck.json");

            if (!Files.exists(venuePath) || !Files.exists(scenarioPath)) {
                throw new IllegalStateException("Demo data files not found in " + base);
            }

            this.venueGraph = objectMapper.readValue(venuePath.toFile(), VenueGraph.class);
            Scenario scenario = objectMapper.readValue(scenarioPath.toFile(), Scenario.class);
            this.scenarioData = toScenarioData(scenario);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load demo JSON data", ex);
        }
    }

    private ScenarioData toScenarioData(Scenario scenario) {
        Frame peakFrame = scenario.frames().stream()
                .max((a, b) -> Double.compare(averageDensity(a), averageDensity(b)))
                .orElseThrow(() -> new IllegalStateException("Scenario has no frames: " + scenario.scenarioId()));
        double confidence = peakFrame.zoneMetrics().stream()
                .mapToDouble(metric -> metric.confidence())
                .average()
                .orElse(0.8);
        return new ScenarioData(
                scenario.eventId(),
                scenario.name(),
                "LIVE",
                scenario.venueId(),
                "MONITORING",
                "RISING",
                confidence,
                peakFrame.zoneMetrics());
    }

    private double averageDensity(Frame frame) {
        return frame.zoneMetrics().stream()
                .mapToDouble(metric -> metric.densityPressure())
                .average()
                .orElse(0.0);
    }

    public VenueGraph getVenueGraph() {
        return venueGraph;
    }

    public ScenarioData getScenarioData() {
        return scenarioData;
    }

    public CrowdShieldProperties getProperties() {
        return properties;
    }
}
