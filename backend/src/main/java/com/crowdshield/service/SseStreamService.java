package com.crowdshield.service;

import com.crowdshield.config.CrowdShieldProperties;
import com.crowdshield.model.PlaybackState;
import com.crowdshield.model.RiskResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class SseStreamService {
    private final RiskScoringService riskScoringService;
    private final ScenarioPlaybackService playbackService;
    private final int heartbeatSeconds;

    public SseStreamService(
            RiskScoringService riskScoringService,
            ScenarioPlaybackService playbackService,
            CrowdShieldProperties properties) {
        this.riskScoringService = riskScoringService;
        this.playbackService = playbackService;
        this.heartbeatSeconds = properties.getSseHeartbeatSeconds();
    }

    public SseEmitter open(String eventId) {
        SseEmitter emitter = new SseEmitter(300_000L);
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("SSE-" + eventId);
            return t;
        });

        Runnable send = () -> {
            try {
                PlaybackState playback = playbackService.getState();
                emitter.send(SseEmitter.event()
                        .name("heartbeat")
                        .data(Map.of("timestamp", Instant.now().toString(), "eventId", eventId)));

                emitter.send(SseEmitter.event()
                        .name("playback_update")
                        .data(playback));

                RiskResponse risk = riskScoringService.calculateCurrentRisk();
                emitter.send(SseEmitter.event()
                        .name("risk_update")
                        .data(Map.of(
                                "timestamp", Instant.now().toString(),
                                "eventId", eventId,
                                "overallRisk", risk.overallRisk(),
                                "zoneRisks", risk.zoneRisks())));
            } catch (IOException ex) {
                try {
                    emitter.completeWithError(ex);
                } catch (Exception ignored) {
                }
            }
        };

        send.run();
        scheduler.scheduleAtFixedRate(send, heartbeatSeconds, heartbeatSeconds, TimeUnit.SECONDS);

        emitter.onCompletion(() -> {
            scheduler.shutdown();
            try {
                scheduler.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }
        });
        emitter.onTimeout(() -> {
            scheduler.shutdownNow();
            try {
                emitter.complete();
            } catch (Exception ignored) {
            }
        });
        emitter.onError(ex -> {
            scheduler.shutdownNow();
        });

        return emitter;
    }
}

