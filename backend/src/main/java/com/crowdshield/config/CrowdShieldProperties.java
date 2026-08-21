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
        private Thresholds thresholds = new Thresholds();
        private Persistence persistence = new Persistence();
        private Hysteresis hysteresis = new Hysteresis();
        private String disclaimer = "Prototype heuristic safety scoring for demonstration purposes only; not certified life-safety software.";

        public Map<String, Double> getWeights() {
            return weights;
        }

        public void setWeights(Map<String, Double> weights) {
            this.weights = weights;
        }

        public Thresholds getThresholds() {
            return thresholds;
        }

        public void setThresholds(Thresholds thresholds) {
            this.thresholds = thresholds;
        }

        public Persistence getPersistence() {
            return persistence;
        }

        public void setPersistence(Persistence persistence) {
            this.persistence = persistence;
        }

        public Hysteresis getHysteresis() {
            return hysteresis;
        }

        public void setHysteresis(Hysteresis hysteresis) {
            this.hysteresis = hysteresis;
        }

        public String getDisclaimer() {
            return disclaimer;
        }

        public void setDisclaimer(String disclaimer) {
            this.disclaimer = disclaimer;
        }

        public static class Thresholds {
            private double moderate = 0.35;
            private double high = 0.55;
            private double critical = 0.75;

            public double getModerate() {
                return moderate;
            }

            public void setModerate(double moderate) {
                this.moderate = moderate;
            }

            public double getHigh() {
                return high;
            }

            public void setHigh(double high) {
                this.high = high;
            }

            public double getCritical() {
                return critical;
            }

            public void setCritical(double critical) {
                this.critical = critical;
            }
        }

        public static class Persistence {
            private int requiredConsecutiveFrames = 2;

            public int getRequiredConsecutiveFrames() {
                return requiredConsecutiveFrames;
            }

            public void setRequiredConsecutiveFrames(int requiredConsecutiveFrames) {
                this.requiredConsecutiveFrames = requiredConsecutiveFrames;
            }
        }

        public static class Hysteresis {
            private int downgradeConsecutiveFrames = 2;
            private int holdSeconds = 45;

            public int getDowngradeConsecutiveFrames() {
                return downgradeConsecutiveFrames;
            }

            public void setDowngradeConsecutiveFrames(int downgradeConsecutiveFrames) {
                this.downgradeConsecutiveFrames = downgradeConsecutiveFrames;
            }

            public int getHoldSeconds() {
                return holdSeconds;
            }

            public void setHoldSeconds(int holdSeconds) {
                this.holdSeconds = holdSeconds;
            }
        }
    }
}
