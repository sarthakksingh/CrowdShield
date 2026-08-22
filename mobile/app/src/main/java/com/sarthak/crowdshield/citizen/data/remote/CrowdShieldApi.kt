package com.sarthak.crowdshield.citizen.data.remote

import com.sarthak.crowdshield.citizen.data.model.AlertsResponseDto
import com.sarthak.crowdshield.citizen.data.model.CurrentAnalyticsResponseDto
import com.sarthak.crowdshield.citizen.data.model.IncidentReportRequestDto
import com.sarthak.crowdshield.citizen.data.model.IncidentReportResponseDto
import com.sarthak.crowdshield.citizen.data.model.RiskResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface CrowdShieldApi {
    @GET("api/risk/current")
    suspend fun getCurrentRisk(): RiskResponseDto

    @GET("api/analytics/current")
    suspend fun getCurrentAnalytics(): CurrentAnalyticsResponseDto

    @GET("api/alerts")
    suspend fun getAlerts(): AlertsResponseDto

    @POST("api/incidents")
    suspend fun submitIncident(@Body request: IncidentReportRequestDto): IncidentReportResponseDto
}
