package com.crowdshield.web;

import com.crowdshield.model.RecommendationsResponse;
import com.crowdshield.service.InterventionEngineService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {
    private final InterventionEngineService interventionEngineService;

    public RecommendationController(InterventionEngineService interventionEngineService) {
        this.interventionEngineService = interventionEngineService;
    }

    @GetMapping("/current")
    public RecommendationsResponse getCurrentRecommendations() {
        return interventionEngineService.getRecommendations();
    }
}
