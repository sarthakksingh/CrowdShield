package com.crowdshield;

import static org.assertj.core.api.Assertions.assertThat;

import com.crowdshield.model.CurrentAnalyticsResponse;
import com.crowdshield.model.RiskLevel;
import com.crowdshield.model.TrendDirection;
import com.crowdshield.model.ZoneAnalytics;
import com.crowdshield.model.ZoneAnalyticsDetailResponse;
import com.crowdshield.service.CrowdAnalyticsService;
import com.crowdshield.service.ScenarioPlaybackService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AnalyticsScenariosTest {

    @Autowired
    private ScenarioPlaybackService playbackService;

    @Autowired
    private CrowdAnalyticsService analyticsService;

    @Test
    void scenario1NormalFlowShowsBalancedFlowAndNoSevereBottlenecks() {
        playbackService.load("s1_normal_flow");
        playbackService.seek(0);

        CurrentAnalyticsResponse analytics = analyticsService.getCurrentAnalytics();
        assertThat(analytics.scenarioId()).isEqualTo("s1_normal_flow");
        assertThat(analytics.bottleneckZones()).isEmpty();
        assertThat(analytics.counterflowZones()).isEmpty();

        for (ZoneAnalytics zone : analytics.zones()) {
            assertThat(zone.netPressure()).isLessThanOrEqualTo(5);
            assertThat(zone.densityPerSqM()).isLessThanOrEqualTo(1.5);
            assertThat(zone.speedDropPct()).isLessThanOrEqualTo(20.0);
            assertThat(zone.movementInstability()).isLessThan(0.30);
            assertThat(zone.bottleneck().isBottleneck()).isFalse();
        }
    }

    @Test
    void scenario2EntryBottleneckDetectsBottleneckWithExplainableRisks() {
        playbackService.load("s2_entry_bottleneck");
        playbackService.seek(40);

        CurrentAnalyticsResponse analytics = analyticsService.getCurrentAnalytics();
        assertThat(analytics.bottleneckZones()).isNotEmpty();

        ZoneAnalytics corridor = analytics.zones().stream()
                .filter(z -> z.zoneId().equals("z-corridor-1"))
                .findFirst()
                .orElseThrow();

        assertThat(corridor.bottleneck().isBottleneck()).isTrue();
        assertThat(corridor.bottleneck().severity()).isIn(RiskLevel.HIGH, RiskLevel.CRITICAL);
        assertThat(corridor.netPressure()).isGreaterThan(50);
        assertThat(corridor.speedDropPct()).isGreaterThan(50.0);
        assertThat(corridor.queueLength()).isGreaterThan(25);
        assertThat(corridor.densityPerSqM()).isGreaterThan(3.5);

        // Explanations why it is risky
        assertThat(corridor.bottleneck().reasons()).contains("inflow_exceeds_outflow");
        assertThat(corridor.bottleneck().reasons()).contains("high_density_accumulation");
        assertThat(corridor.bottleneck().reasons()).contains("severe_speed_drop");
    }

    @Test
    void scenario3CounterflowPanicDetectsSevereCounterflowAndHighInstability() {
        playbackService.load("s3_counterflow_panic");
        playbackService.seek(0);

        CurrentAnalyticsResponse analytics = analyticsService.getCurrentAnalytics();
        assertThat(analytics.counterflowZones()).isNotEmpty();

        ZoneAnalytics corridor = analytics.zones().stream()
                .filter(z -> z.zoneId().equals("z-corridor-1"))
                .findFirst()
                .orElseThrow();

        assertThat(corridor.counterflow().isCounterflowDetected()).isTrue();
        assertThat(corridor.counterflow().severity()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(corridor.movementInstability()).isGreaterThan(0.70);
        assertThat(corridor.instabilityLevel()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(corridor.counterflow().reasons()).contains("critical_counterflow_conflict");
    }

    @Test
    void scenario4PostInterventionRecoveryShowsCrowdDispersalAndFallingDensity() {
        playbackService.load("s4_post_intervention_recovery");

        // Beginning of recovery
        playbackService.seek(0);
        ZoneAnalytics t0 = analyticsService.getZoneAnalytics("z-corridor-1").analytics();

        // After intervention takes effect (e.g. at offset 75 or 100)
        playbackService.seek(100);
        ZoneAnalyticsDetailResponse t100Response = analyticsService.getZoneAnalytics("z-corridor-1");
        ZoneAnalytics t100 = t100Response.analytics();

        // Verify density decreased
        assertThat(t100.densityPerSqM()).isLessThan(t0.densityPerSqM());
        assertThat(t100.densityTrend()).isEqualTo(TrendDirection.FALLING);

        // Verify queue decreased
        assertThat(t100.queueLength()).isLessThan(t0.queueLength());
        assertThat(t100.queueTrend()).isEqualTo(TrendDirection.FALLING);

        // Verify net pressure turned negative or cleared (outflow > inflow)
        assertThat(t100.netPressure()).isLessThan(t0.netPressure());

        // Verify speed recovered
        assertThat(t100.speedMpsP50()).isGreaterThan(t0.speedMpsP50());
        assertThat(t100.speedDropPct()).isLessThan(t0.speedDropPct());

        // Verify timeline history has entries from t=0 to t=100
        assertThat(t100Response.timeline()).hasSizeGreaterThanOrEqualTo(4);
    }
}
