export type RiskLevel = 'LOW' | 'MODERATE' | 'HIGH' | 'CRITICAL';

export type RecommendationActionType =
  | 'RESTRICT_GATE'
  | 'OPEN_EXIT'
  | 'OPEN_ROUTE'
  | 'REDIRECT_INFLOW'
  | 'DEPLOY_PERSONNEL'
  | 'BROADCAST_ALERT';

export interface EventInfo {
  eventId: string;
  name: string;
  venueId: string;
  venueName?: string;
  status: string;
}

export interface PlaybackState {
  scenarioId: string;
  scenarioName: string;
  durationSec: number;
  currentOffsetSec: number;
  playing: boolean;
  playbackSpeed: number;
  availableScenarios: string[];
}

export interface ZoneMetric {
  zoneId: string;
  densityPressure: number;
  inflowOutflowImbalance: number;
  movementInstability: number;
  opposingMovement: number;
  bottleneckPressure: number;
  routeAvailability: number;
  densityPerSqM: number;
  flowInPerMin: number;
  flowOutPerMin: number;
  speedMpsP50: number;
  queueLength: number;
  occupancyPct: number;
  dataQuality: string;
  confidence: number;
  reasons: string[];
}

export interface ZoneRisk {
  zoneId: string;
  score: number;
  level: RiskLevel;
  trend: string;
  confidence: number;
  reasons: string[];
  contributions: Record<string, number>;
  factorDescriptions?: Record<string, string>;
  horizon?: string;
  holdUntil?: string;
  disclaimer?: string;
}

export interface OverallRisk {
  score: number;
  level: RiskLevel;
  trend: string;
  confidence: number;
  reasons: string[];
  contributions: Record<string, number>;
  factorDescriptions?: Record<string, string>;
  horizon?: string;
  holdUntil?: string;
  disclaimer?: string;
}

export interface RiskResponse {
  overallRisk: OverallRisk;
  zoneRisks: ZoneRisk[];
  disclaimer?: string;
}

export interface BottleneckInfo {
  isBottleneck: boolean;
  severity: RiskLevel;
  inflowRate: number;
  outflowRate: number;
  netRate: number;
  density: number;
  queueLength: number;
  speedDropPct: number;
  reasons: string[];
}

export interface CounterflowInfo {
  isCounterflowDetected: boolean;
  severity: RiskLevel;
  opposingMovementScore: number;
  turbulenceIndex: number;
  reasons: string[];
}

export interface ZoneAnalytics {
  zoneId: string;
  densityPerSqM: number;
  densityTrend: string;
  inflowPerMin: number;
  inflowTrend: string;
  outflowPerMin: number;
  outflowTrend: string;
  netPressure: number;
  netPressureTrend: string;
  speedDropPct: number;
  queueLength: number;
  queueGrowth: number;
  queueTrend: string;
  movementInstability: number;
  instabilityLevel: RiskLevel;
  bottleneck: BottleneckInfo;
  counterflow: CounterflowInfo;
  confidence: number;
  timestamp: string;
}

export interface ZoneAnalyticsTimelinePoint {
  offsetSec: number;
  timestamp: string;
  densityPerSqM: number;
  inflowPerMin: number;
  outflowPerMin: number;
  netPressure: number;
  queueLength: number;
  speedDropPct: number;
  movementInstability: number;
  isBottleneck: boolean;
  bottleneckSeverity: RiskLevel;
  isCounterflow: boolean;
}

export interface ZoneAnalyticsDetailResponse {
  timestamp: string;
  eventId: string;
  scenarioId: string;
  currentOffsetSec: number;
  analytics: ZoneAnalytics;
  timeline: ZoneAnalyticsTimelinePoint[];
  current?: ZoneAnalytics;
  timelineHistory?: ZoneAnalyticsTimelinePoint[];
}

export interface CurrentAnalyticsResponse {
  timestamp: string;
  eventId: string;
  scenarioId: string;
  currentOffsetSec: number;
  zones: ZoneAnalytics[];
  detectedBottlenecks: string[];
  counterflowZones: string[];
  summary: {
    totalZones: number;
    bottleneckCount: number;
    counterflowCount: number;
    highestNetPressureZone: string;
    maxDensity: number;
  };
}

export interface AlertRecord {
  id: string;
  zoneId: string;
  severity: string;
  message: string;
  reasons: string[];
  timestamp: string;
  dedupeKey: string;
}

export interface Recommendation {
  id: string;
  actionType: RecommendationActionType;
  targetZoneId: string;
  targetType: string;
  title: string;
  reason: string;
  expectedImpact: string;
  confidence: number;
  urgency: RiskLevel;
  suggestedMessage: string;
  rank: number;
}

export interface RecommendationsResponse {
  timestamp: string;
  eventId: string;
  scenarioId: string;
  currentOffsetSec: number;
  overallRiskLevel: RiskLevel;
  recommendations: Recommendation[];
  advisoryNotice: string;
  summary: {
    totalRecommendations: number;
    topAction: string;
    topTarget: string;
    highestUrgency: string;
    criticalActionCount: number;
  };
}

export interface SimulationAction {
  type: string;
  targetId: string;
  atOffsetSec: number;
}

export interface SimulationRequest {
  eventId: string;
  scenarioName: string;
  actions: SimulationAction[];
  horizonSec: number;
}

export interface ZoneImpact {
  zoneId: string;
  baselineScore: number;
  projectedScore: number;
  delta: number;
  status: 'IMPROVED' | 'TRANSFERRED_RISK' | 'UNCHANGED' | 'WORSENED';
  notes: string;
}

export interface SimulationResponse {
  timestamp: string;
  eventId: string;
  simulationId: string;
  scenarioName: string;
  heuristic: boolean;
  disclaimer: string;
  baseline: {
    peakRisk: number;
    overallScore: number;
    level: RiskLevel;
    timeToPeakSec: number;
    zoneRisks?: ZoneRisk[];
  };
  projected: {
    peakRisk: number;
    overallScore: number;
    level: RiskLevel;
    timeToPeakSec: number;
    zoneRisks?: ZoneRisk[];
  };
  delta: {
    peakRisk: number;
    overallScore: number;
    estimatedClearanceSec: number;
  };
  zoneImpacts: ZoneImpact[];
  riskTransferNotes: string[];
  verdict: 'IMPROVED' | 'WORSENED' | 'NO_SIGNIFICANT_CHANGE';
  recommendation: string;
}

export interface HorizonRiskForecast {
  horizonSec: number;
  score: number;
  level: RiskLevel;
  deltaFromCurrent: number;
}

export interface ZoneForecast {
  zoneId: string;
  currentScore: number;
  currentLevel: RiskLevel;
  horizons: Record<string, HorizonRiskForecast>;
  trend: 'RISING' | 'FALLING' | 'STABLE';
  method: string;
  methodsAgree: boolean;
  confidence: number;
  horizonEstimate: string;
  reasons: string[];
}

export interface OverallForecast {
  currentScore: number;
  currentLevel: RiskLevel;
  horizons: Record<string, HorizonRiskForecast>;
  trend: 'RISING' | 'FALLING' | 'STABLE';
  confidence: number;
  timeToCriticalSec: number;
  horizonEstimate: string;
  reasons: string[];
}

export interface ForecastResponse {
  timestamp: string;
  eventId: string;
  scenarioId: string;
  currentOffsetSec: number;
  horizonsSec: number[];
  overallForecast: OverallForecast;
  zoneForecasts: ZoneForecast[];
  disclaimer: string;
}

