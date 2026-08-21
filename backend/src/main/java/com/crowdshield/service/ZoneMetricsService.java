package com.crowdshield.service;

import com.crowdshield.model.ZoneMetric;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ZoneMetricsService {
    private final ScenarioPlaybackService playbackService;

    public ZoneMetricsService(ScenarioPlaybackService playbackService) {
        this.playbackService = playbackService;
    }

    public List<ZoneMetric> getCurrentZoneMetrics() {
        return playbackService.getState().currentZoneMetrics();
    }
}
