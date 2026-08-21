package com.crowdshield.web;

import com.crowdshield.model.CurrentAnalyticsResponse;
import com.crowdshield.model.ZoneAnalyticsDetailResponse;
import com.crowdshield.service.CrowdAnalyticsService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
    private final CrowdAnalyticsService analyticsService;

    public AnalyticsController(CrowdAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/current")
    public CurrentAnalyticsResponse getCurrentAnalytics() {
        return analyticsService.getCurrentAnalytics();
    }

    @GetMapping("/zones/{zoneId}")
    public ZoneAnalyticsDetailResponse getZoneAnalytics(@PathVariable String zoneId) {
        return analyticsService.getZoneAnalytics(zoneId);
    }
}
