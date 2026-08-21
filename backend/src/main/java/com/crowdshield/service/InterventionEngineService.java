package com.crowdshield.service;

import com.crowdshield.model.CurrentAnalyticsResponse;
import com.crowdshield.model.PlaybackState;
import com.crowdshield.model.Recommendation;
import com.crowdshield.model.RecommendationsResponse;
import com.crowdshield.model.RiskLevel;
import com.crowdshield.model.RiskResponse;
import com.crowdshield.service.intervention.AnnouncementAdvisor;
import com.crowdshield.service.intervention.GateAdvisor;
import com.crowdshield.service.intervention.PersonnelAdvisor;
import com.crowdshield.service.intervention.RouteAdvisor;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class InterventionEngineService {
    private static final String ADVISORY_NOTICE =
            "All recommendations are automated decision-support advisories for authorized operations personnel only; does not perform automatic physical actuation.";

    private final RiskScoringService riskScoringService;
    private final CrowdAnalyticsService analyticsService;
    private final ScenarioPlaybackService playbackService;
    private final EventStateService eventStateService;

    private final GateAdvisor gateAdvisor;
    private final RouteAdvisor routeAdvisor;
    private final PersonnelAdvisor personnelAdvisor;
    private final AnnouncementAdvisor announcementAdvisor;

    public InterventionEngineService(
            RiskScoringService riskScoringService,
            CrowdAnalyticsService analyticsService,
            ScenarioPlaybackService playbackService,
            EventStateService eventStateService,
            GateAdvisor gateAdvisor,
            RouteAdvisor routeAdvisor,
            PersonnelAdvisor personnelAdvisor,
            AnnouncementAdvisor announcementAdvisor) {
        this.riskScoringService = riskScoringService;
        this.analyticsService = analyticsService;
        this.playbackService = playbackService;
        this.eventStateService = eventStateService;
        this.gateAdvisor = gateAdvisor;
        this.routeAdvisor = routeAdvisor;
        this.personnelAdvisor = personnelAdvisor;
        this.announcementAdvisor = announcementAdvisor;
    }

    public RecommendationsResponse getRecommendations() {
        PlaybackState state = playbackService.getState();
        String eventId = eventStateService.getEventId();
        RiskResponse currentRisk = riskScoringService.calculateCurrentRisk();
        CurrentAnalyticsResponse currentAnalytics = analyticsService.getCurrentAnalytics();

        List<Recommendation> rawList = new ArrayList<>();
        rawList.addAll(gateAdvisor.evaluate(currentRisk, currentAnalytics));
        rawList.addAll(routeAdvisor.evaluate(currentRisk, currentAnalytics));
        rawList.addAll(personnelAdvisor.evaluate(currentRisk, currentAnalytics));
        rawList.addAll(announcementAdvisor.evaluate(currentRisk, currentAnalytics));

        // Rank recommendations: Urgency descending, confidence descending
        rawList.sort(Comparator
                .comparing((Recommendation r) -> r.urgency().ordinal()).reversed()
                .thenComparing(Comparator.comparingDouble(Recommendation::confidence).reversed()));

        List<Recommendation> rankedList = new ArrayList<>();
        for (int i = 0; i < rawList.size(); i++) {
            Recommendation r = rawList.get(i);
            rankedList.add(new Recommendation(
                    r.id(),
                    r.actionType(),
                    r.targetZoneId(),
                    r.targetType(),
                    r.title(),
                    r.reason(),
                    r.expectedImpact(),
                    r.confidence(),
                    r.urgency(),
                    r.suggestedMessage(),
                    i + 1));
        }

        // Summary
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalRecommendations", rankedList.size());
        summary.put("topAction", rankedList.isEmpty() ? null : rankedList.get(0).actionType().name());
        summary.put("topTarget", rankedList.isEmpty() ? null : rankedList.get(0).targetZoneId());
        summary.put("highestUrgency", rankedList.isEmpty() ? RiskLevel.LOW.name() : rankedList.get(0).urgency().name());
        summary.put("criticalActionCount", rankedList.stream().filter(r -> r.urgency() == RiskLevel.CRITICAL).count());

        return new RecommendationsResponse(
                Instant.now().toString(),
                eventId,
                state.scenarioId(),
                state.currentOffsetSec(),
                currentRisk.overallRisk().level(),
                rankedList,
                ADVISORY_NOTICE,
                summary);
    }
}
