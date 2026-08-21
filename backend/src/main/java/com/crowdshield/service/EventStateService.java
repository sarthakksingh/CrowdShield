package com.crowdshield.service;

import com.crowdshield.model.ScenarioData;
import org.springframework.stereotype.Service;

@Service
public class EventStateService {
    private final ScenarioPlaybackService playbackService;
    private final DemoDataService demoDataService;

    public EventStateService(ScenarioPlaybackService playbackService, DemoDataService demoDataService) {
        this.playbackService = playbackService;
        this.demoDataService = demoDataService;
    }

    public ScenarioData currentEvent() {
        return demoDataService.getScenarioData();
    }

    public String getEventId() {
        return playbackService.getState().scenarioId();
    }
}
