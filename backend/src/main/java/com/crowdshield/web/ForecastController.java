package com.crowdshield.web;

import com.crowdshield.model.ForecastResponse;
import com.crowdshield.service.ShortHorizonForecastService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/forecast")
public class ForecastController {
    private final ShortHorizonForecastService forecastService;

    public ForecastController(ShortHorizonForecastService forecastService) {
        this.forecastService = forecastService;
    }

    @GetMapping("/current")
    public ForecastResponse getCurrentForecast() {
        return forecastService.getCurrentForecast();
    }
}
