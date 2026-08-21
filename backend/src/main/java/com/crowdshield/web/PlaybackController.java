package com.crowdshield.web;

import com.crowdshield.model.PlaybackState;
import com.crowdshield.model.Scenario;
import com.crowdshield.service.ScenarioPlaybackService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api")
public class PlaybackController {
    private final ScenarioPlaybackService playbackService;

    public PlaybackController(ScenarioPlaybackService playbackService) {
        this.playbackService = playbackService;
    }

    @GetMapping("/scenarios")
    public Map<String, Object> listScenarios() {
        List<Scenario> scenarios = playbackService.listScenarios();
        return Map.of(
                "timestamp", java.time.Instant.now().toString(),
                "scenarios", scenarios);
    }

    @GetMapping("/playback")
    public PlaybackState getPlaybackState() {
        return playbackService.getState();
    }

    @PostMapping("/playback/load")
    @ResponseStatus(HttpStatus.OK)
    public PlaybackState load(@Valid @RequestBody PlaybackLoadRequest request) {
        playbackService.load(request.scenarioId());
        return playbackService.getState();
    }

    @PostMapping("/playback/play")
    public PlaybackState play(@Valid @RequestBody PlaybackPlayRequest request) {
        playbackService.play(request.speed());
        return playbackService.getState();
    }

    @PostMapping("/playback/pause")
    public PlaybackState pause() {
        playbackService.pause();
        return playbackService.getState();
    }

    @PostMapping("/playback/reset")
    public PlaybackState reset() {
        playbackService.reset();
        return playbackService.getState();
    }

    @PostMapping("/playback/seek")
    public PlaybackState seek(@Valid @RequestBody PlaybackSeekRequest request) {
        playbackService.seek(request.offsetSec());
        return playbackService.getState();
    }

    public record PlaybackLoadRequest(@NotBlank String scenarioId) {}

    public record PlaybackPlayRequest(@Min(0) double speed) {}

    public record PlaybackSeekRequest(@Min(0) int offsetSec) {}
}
