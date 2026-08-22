package com.sarthak.crowdshield.citizen.data.model

import com.google.gson.annotations.SerializedName

data class OverallRiskDto(
    @SerializedName("score") val score: Double = 0.0,
    @SerializedName("level") val level: String = "LOW",
    @SerializedName("trend") val trend: String = "STABLE",
    @SerializedName("confidence") val confidence: Double = 0.85,
    @SerializedName("horizon") val horizon: String? = null,
    @SerializedName("reasons") val reasons: List<String>? = null
)

data class ZoneRiskDto(
    @SerializedName("zoneId") val zoneId: String,
    @SerializedName("score") val score: Double = 0.0,
    @SerializedName("level") val level: String = "LOW",
    @SerializedName("trend") val trend: String = "STABLE",
    @SerializedName("confidence") val confidence: Double = 0.85
)

data class RiskResponseDto(
    @SerializedName("timestamp") val timestamp: String? = null,
    @SerializedName("eventId") val eventId: String? = null,
    @SerializedName("overallRisk") val overallRisk: OverallRiskDto? = null,
    @SerializedName("zoneRisks") val zoneRisks: List<ZoneRiskDto>? = null,
    @SerializedName("disclaimer") val disclaimer: String? = null
)

data class BottleneckInfoDto(
    @SerializedName("isBottleneck") val isBottleneck: Boolean = false,
    @SerializedName("severity") val severity: String? = null,
    @SerializedName("reasons") val reasons: List<String>? = null
)

data class CounterflowInfoDto(
    @SerializedName("isCounterflowDetected") val isCounterflowDetected: Boolean = false,
    @SerializedName("reasons") val reasons: List<String>? = null
)

data class ZoneAnalyticsDto(
    @SerializedName("zoneId") val zoneId: String,
    @SerializedName("densityPerSqM") val densityPerSqM: Double = 0.0,
    @SerializedName("densityTrend") val densityTrend: String? = "STABLE",
    @SerializedName("inflowPerMin") val inflowPerMin: Int = 0,
    @SerializedName("outflowPerMin") val outflowPerMin: Int = 0,
    @SerializedName("netPressure") val netPressure: Int = 0,
    @SerializedName("queueLength") val queueLength: Int = 0,
    @SerializedName("speedDropPct") val speedDropPct: Double = 0.0,
    @SerializedName("movementInstability") val movementInstability: Double = 0.0,
    @SerializedName("bottleneck") val bottleneck: BottleneckInfoDto? = null,
    @SerializedName("counterflow") val counterflow: CounterflowInfoDto? = null
)

data class CurrentAnalyticsResponseDto(
    @SerializedName("timestamp") val timestamp: String? = null,
    @SerializedName("eventId") val eventId: String? = null,
    @SerializedName("scenarioId") val scenarioId: String? = null,
    @SerializedName("currentOffsetSec") val currentOffsetSec: Int = 0,
    @SerializedName("zones") val zones: List<ZoneAnalyticsDto>? = null
)

data class AlertItemDto(
    @SerializedName("id") val id: String,
    @SerializedName("zoneId") val zoneId: String,
    @SerializedName("severity") val severity: String = "LOW",
    @SerializedName("message") val message: String = "",
    @SerializedName("reasons") val reasons: List<String>? = null,
    @SerializedName("timestamp") val timestamp: String = "",
    @SerializedName("dedupeKey") val dedupeKey: String? = null
)

data class AlertsResponseDto(
    @SerializedName("timestamp") val timestamp: String? = null,
    @SerializedName("eventId") val eventId: String? = null,
    @SerializedName("alerts") val alerts: List<AlertItemDto>? = null
)

data class IncidentLocationDto(
    @SerializedName("lat") val lat: Double = 28.1,
    @SerializedName("lon") val lon: Double = 77.2,
    @SerializedName("zoneId") val zoneId: String
)

data class IncidentReportRequestDto(
    @SerializedName("eventId") val eventId: String = "event-tech-nova-2026",
    @SerializedName("reporterType") val reporterType: String = "CITIZEN",
    @SerializedName("location") val location: IncidentLocationDto,
    @SerializedName("category") val category: String,
    @SerializedName("severity") val severity: String,
    @SerializedName("description") val description: String,
    @SerializedName("reportedAt") val reportedAt: String
)

data class IncidentReportResponseDto(
    @SerializedName("timestamp") val timestamp: String? = null,
    @SerializedName("eventId") val eventId: String? = null,
    @SerializedName("incidentId") val incidentId: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("triageQueuePosition") val triageQueuePosition: Int? = null
)
