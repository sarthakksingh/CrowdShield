package com.crowdshield;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.crowdshield.model.CurrentAnalyticsResponse;
import com.crowdshield.model.RiskLevel;
import com.crowdshield.model.TrendDirection;
import com.crowdshield.model.ZoneAnalytics;
import com.crowdshield.model.ZoneAnalyticsDetailResponse;
import com.crowdshield.service.CrowdAnalyticsService;
import com.crowdshield.service.ScenarioPlaybackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CrowdAnalyticsServiceTest {

    @Autowired
    private ScenarioPlaybackService playbackService;

    @Autowired
    private CrowdAnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        playbackService.load("s2_entry_bottleneck");
        playbackService.seek(0);
    }

    @Test
    void netPressureIsComputedCorrectlyAsInflowMinusOutflow() {
        playbackService.load("s2_entry_bottleneck");
        playbackService.seek(0);

        CurrentAnalyticsResponse response = analyticsService.getCurrentAnalytics();
        ZoneAnalytics corridor = response.zones().stream()
                .filter(z -> z.zoneId().equals("z-corridor-1"))
                .findFirst()
                .orElseThrow();

        // In s2_entry_bottleneck t=0: flowIn=100, flowOut=50 -> netPressure = +50
        assertThat(corridor.flowInPerMin()).isEqualTo(100);
        assertThat(corridor.flowOutPerMin()).isEqualTo(50);
        assertThat(corridor.netPressure()).isEqualTo(50);
    }

    @Test
    void speedDropPercentageCalculatedRelativeToFreeFlowBaseline() {
        playbackService.load("s2_entry_bottleneck");
        playbackService.seek(0);

        ZoneAnalyticsDetailResponse detail = analyticsService.getZoneAnalytics("z-corridor-1");
        // speed = 0.75 vs 1.5 baseline -> 50% drop
        assertThat(detail.analytics().speedMpsP50()).isEqualTo(0.75);
        assertThat(detail.analytics().speedDropPct()).isEqualTo(50.0);
    }

    @Test
    void bottleneckDetectorIdentifiesHighRiskZoneAndProvidesExplainabilityReasons() {
        playbackService.load("s2_entry_bottleneck");
        playbackService.seek(40); // high congestion frame

        CurrentAnalyticsResponse response = analyticsService.getCurrentAnalytics();
        assertThat(response.bottleneckZones()).isNotEmpty();

        ZoneAnalytics corridor = response.zones().stream()
                .filter(z -> z.zoneId().equals("z-corridor-1"))
                .findFirst()
                .orElseThrow();

        assertThat(corridor.bottleneck().isBottleneck()).isTrue();
        assertThat(corridor.bottleneck().severity()).isIn(RiskLevel.HIGH, RiskLevel.CRITICAL);
        assertThat(corridor.bottleneck().reasons()).isNotEmpty();
        // Check that reasons explain WHY it is risky
        assertThat(corridor.bottleneck().reasons()).anyMatch(r ->
                r.equals("inflow_exceeds_outflow") ||
                r.equals("high_density_accumulation") ||
                r.equals("severe_speed_drop") ||
                r.equals("bottleneck_pressure_critical") ||
                r.equals("growing_queue"));
    }

    @Test
    void counterflowDetectorIdentifiesOpposingMovementInPanicScenario() {
        playbackService.load("s3_counterflow_panic");
        playbackService.seek(0);

        CurrentAnalyticsResponse response = analyticsService.getCurrentAnalytics();
        assertThat(response.counterflowZones()).isNotEmpty();

        ZoneAnalytics corridor = response.zones().stream()
                .filter(z -> z.zoneId().equals("z-corridor-1"))
                .findFirst()
                .orElseThrow();

        assertThat(corridor.counterflow().isCounterflowDetected()).isTrue();
        assertThat(corridor.counterflow().severity()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(corridor.counterflow().reasons()).contains("critical_counterflow_conflict");
    }

    @Test
    void movementInstabilityScoreEvaluatedCorrectly() {
        playbackService.load("s3_counterflow_panic");
        playbackService.seek(0);

        ZoneAnalyticsDetailResponse corridor = analyticsService.getZoneAnalytics("z-corridor-1");
        assertThat(corridor.analytics().movementInstability()).isGreaterThan(0.75);
        assertThat(corridor.analytics().instabilityLevel()).isEqualTo(RiskLevel.CRITICAL);
    }

    @Test
    void densityAndQueueTrendsEvolveWhenPlaybackAdvances() {
        playbackService.load("s2_entry_bottleneck");
        playbackService.seek(0);
        ZoneAnalytics t0 = analyticsService.getZoneAnalytics("z-corridor-1").analytics();

        playbackService.seek(40);
        ZoneAnalytics t40 = analyticsService.getZoneAnalytics("z-corridor-1").analytics();

        assertThat(t40.densityPerSqM()).isGreaterThan(t0.densityPerSqM());
        assertThat(t40.densityTrend()).isEqualTo(TrendDirection.RISING);
        assertThat(t40.queueLength()).isGreaterThan(t0.queueLength());
        assertThat(t40.queueTrend()).isEqualTo(TrendDirection.RISING);
    }

    @Test
    void getZoneAnalyticsForInvalidZoneThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> analyticsService.getZoneAnalytics("invalid-zone-id"));
    }
}
