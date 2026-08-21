package com.crowdshield.config;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "crowdshield")
public class CrowdShieldProperties {
    private String demoDataDir = "../demo-data";
    private int hysteresisHoldSeconds = 45;
    private int sseHeartbeatSeconds = 10;
    private Risk risk = new Risk();

    public String getDemoDataDir() {
        return demoDataDir;
    }

    public void setDemoDataDir(String demoDataDir) {
        this.demoDataDir = demoDataDir;
    }

    public int getHysteresisHoldSeconds() {
        return hysteresisHoldSeconds;
    }

    public void setHysteresisHoldSeconds(int hysteresisHoldSeconds) {
        this.hysteresisHoldSeconds = hysteresisHoldSeconds;
    }

    public int getSseHeartbeatSeconds() {
        return sseHeartbeatSeconds;
    }

    public void setSseHeartbeatSeconds(int sseHeartbeatSeconds) {
        this.sseHeartbeatSeconds = sseHeartbeatSeconds;
    }

    public Risk getRisk() {
        return risk;
    }

    public void setRisk(Risk risk) {
        this.risk = risk;
    }

    public static class Risk {
        private Map<String, Double> weights = new HashMap<>();

        public Map<String, Double> getWeights() {
            return weights;
        }

        public void setWeights(Map<String, Double> weights) {
            this.weights = weights;
        }
    }
}
