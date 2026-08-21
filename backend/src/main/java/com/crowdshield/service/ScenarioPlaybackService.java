package com.crowdshield.service;

import com.crowdshield.model.Frame;
import com.crowdshield.model.Scenario;
import com.crowdshield.model.PlaybackState;
import com.crowdshield.model.ZoneMetric;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class ScenarioPlaybackService {
    private final ObjectMapper objectMapper;
    private final String demoDataDir;
    private final Map<String, Scenario> scenarios = new ConcurrentHashMap<>();
    private volatile PlaybackStateHolder state;

    public ScenarioPlaybackService(ObjectMapper objectMapper, DemoDataService demoDataService) {
        this.objectMapper = objectMapper;
        this.demoDataDir = demoDataService.getProperties().getDemoDataDir();
        loadAllScenarios();
        initializePlayback();
    }

    private void loadAllScenarios() {
        try {
            Path scenariosDir = Paths.get(demoDataDir).toAbsolutePath().normalize().resolve("scenarios");
            if (Files.exists(scenariosDir)) {
                Files.list(scenariosDir)
                        .filter(p -> p.toString().endsWith(".json"))
                        .forEach(p -> {
                            try {
                                Scenario scenario = objectMapper.readValue(p.toFile(), Scenario.class);
                                scenarios.put(scenario.scenarioId(), scenario);
                            } catch (IOException ex) {
                                throw new IllegalStateException("Unable to load scenario from " + p, ex);
                            }
                        });
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read scenarios directory", ex);
        }
    }

    private void initializePlayback() {
        Scenario initial = scenarios.get("s2_entry_bottleneck");
        if (initial == null) {
            initial = scenarios.values().iterator().next();
        }
        this.state = new PlaybackStateHolder(initial, 0, 1.0, false);
    }

    public List<Scenario> listScenarios() {
        return new ArrayList<>(scenarios.values());
    }

    public void load(String scenarioId) {
        Scenario scenario = scenarios.get(scenarioId);
        if (scenario == null) {
            throw new IllegalArgumentException("Scenario not found: " + scenarioId);
        }
        state = new PlaybackStateHolder(scenario, 0, 1.0, false);
    }

    public void play(double speed) {
        state.play(speed);
    }

    public void pause() {
        state.pause();
    }

    public void reset() {
        state.reset();
    }

    public void seek(int offsetSec) {
        state.seek(offsetSec);
    }

    public PlaybackState getState() {
        PlaybackStateHolder current = state;
        current.updateIfPlaying();
        return current.toPlaybackState();
    }

    private static class PlaybackStateHolder {
        private final Scenario scenario;
        private int currentOffsetSec;
        private double speed;
        private boolean playing;
        private Instant startedAt;
        private Instant lastPlayStartedAt;

        PlaybackStateHolder(Scenario scenario, int offsetSec, double speed, boolean playing) {
            this.scenario = scenario;
            this.currentOffsetSec = offsetSec;
            this.speed = speed;
            this.playing = playing;
            this.startedAt = Instant.now();
            this.lastPlayStartedAt = null;
        }

        void play(double speed) {
            this.speed = speed;
            this.playing = true;
            this.lastPlayStartedAt = Instant.now();
        }

        void pause() {
            this.playing = false;
        }

        void reset() {
            this.currentOffsetSec = 0;
            this.playing = false;
            this.lastPlayStartedAt = null;
            this.startedAt = Instant.now();
        }

        void seek(int offsetSec) {
            this.currentOffsetSec = Math.min(offsetSec, scenario.durationSec());
            this.lastPlayStartedAt = Instant.now();
        }

        void updateIfPlaying() {
            if (playing && lastPlayStartedAt != null) {
                long elapsedMs = Instant.now().toEpochMilli() - lastPlayStartedAt.toEpochMilli();
                int elapsedSec = (int) (elapsedMs / 1000.0 * speed);
                currentOffsetSec = Math.min(currentOffsetSec + elapsedSec, scenario.durationSec());
                if (currentOffsetSec >= scenario.durationSec()) {
                    playing = false;
                }
                lastPlayStartedAt = Instant.now();
            }
        }

        PlaybackState toPlaybackState() {
            String status = playing ? "PLAYING" : "PAUSED";
            Frame currentFrame = findCurrentFrame();
            List<ZoneMetric> metrics = currentFrame != null ? currentFrame.zoneMetrics() : Collections.emptyList();
            return new PlaybackState(
                    scenario.scenarioId(),
                    status,
                    currentOffsetSec,
                    scenario.durationSec(),
                    speed,
                    startedAt,
                    Instant.now(),
                    currentFrame,
                    metrics);
        }

        private Frame findCurrentFrame() {
            Frame result = null;
            for (Frame frame : scenario.frames()) {
                if (frame.offsetSec() <= currentOffsetSec) {
                    result = frame;
                } else {
                    break;
                }
            }
            return result;
        }
    }
}
