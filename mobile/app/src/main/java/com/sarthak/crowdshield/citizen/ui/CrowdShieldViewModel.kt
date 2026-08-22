package com.sarthak.crowdshield.citizen.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sarthak.crowdshield.citizen.data.model.AlertItemDto
import com.sarthak.crowdshield.citizen.data.model.IncidentLocationDto
import com.sarthak.crowdshield.citizen.data.model.IncidentReportRequestDto
import com.sarthak.crowdshield.citizen.data.model.IncidentReportResponseDto
import com.sarthak.crowdshield.citizen.data.model.OverallRiskDto
import com.sarthak.crowdshield.citizen.data.model.ZoneAnalyticsDto
import com.sarthak.crowdshield.citizen.data.remote.ApiConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant

data class CitizenUiState(
    val overallRisk: OverallRiskDto? = null,
    val zones: List<ZoneAnalyticsDto> = emptyList(),
    val alerts: List<AlertItemDto> = emptyList(),
    val isLiveRiskLoading: Boolean = true,
    val isAlertsLoading: Boolean = true,
    val liveRiskError: String? = null,
    val alertsError: String? = null,
    val isSubmittingIncident: Boolean = false,
    val incidentSuccessResponse: IncidentReportResponseDto? = null,
    val incidentError: String? = null,
    val lastRefreshedAt: String = ""
)

class CrowdShieldViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CitizenUiState())
    val uiState: StateFlow<CitizenUiState> = _uiState.asStateFlow()

    init {
        startPolling()
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (isActive) {
                refreshAllData()
                delay(3000) // Poll every 3 seconds for live updates
            }
        }
    }

    fun refreshAllData() {
        fetchLiveRisk()
        fetchAlerts()
    }

    fun fetchLiveRisk() {
        viewModelScope.launch {
            try {
                val api = ApiConfig.getApi()
                val riskDeferred = api.getCurrentRisk()
                val analyticsDeferred = api.getCurrentAnalytics()

                _uiState.update { current ->
                    current.copy(
                        overallRisk = riskDeferred.overallRisk,
                        zones = analyticsDeferred.zones ?: emptyList(),
                        isLiveRiskLoading = false,
                        liveRiskError = null,
                        lastRefreshedAt = Instant.now().toString().substring(11, 19)
                    )
                }
            } catch (e: Exception) {
                _uiState.update { current ->
                    current.copy(
                        isLiveRiskLoading = false,
                        liveRiskError = e.localizedMessage ?: "Failed to connect to CrowdShield API"
                    )
                }
            }
        }
    }

    fun fetchAlerts() {
        viewModelScope.launch {
            try {
                val api = ApiConfig.getApi()
                val alertsResponse = api.getAlerts()
                val sortedAlerts = (alertsResponse.alerts ?: emptyList()).sortedByDescending { it.timestamp }

                _uiState.update { current ->
                    current.copy(
                        alerts = sortedAlerts,
                        isAlertsLoading = false,
                        alertsError = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { current ->
                    current.copy(
                        isAlertsLoading = false,
                        alertsError = e.localizedMessage ?: "Failed to fetch alerts"
                    )
                }
            }
        }
    }

    fun submitIncident(
        zoneId: String,
        category: String,
        severity: String,
        description: String
    ) {
        if (description.isBlank()) {
            _uiState.update { it.copy(incidentError = "Please provide a short description of the incident.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingIncident = true, incidentError = null, incidentSuccessResponse = null) }
            try {
                val request = IncidentReportRequestDto(
                    eventId = "event-tech-nova-2026",
                    reporterType = "CITIZEN",
                    location = IncidentLocationDto(
                        lat = 28.1,
                        lon = 77.2,
                        zoneId = zoneId
                    ),
                    category = category,
                    severity = severity,
                    description = description.trim(),
                    reportedAt = Instant.now().toString()
                )

                val response = ApiConfig.getApi().submitIncident(request)
                _uiState.update {
                    it.copy(
                        isSubmittingIncident = false,
                        incidentSuccessResponse = response,
                        incidentError = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmittingIncident = false,
                        incidentError = e.localizedMessage ?: "Submission failed. Please check network connection."
                    )
                }
            }
        }
    }

    fun clearIncidentStatus() {
        _uiState.update { it.copy(incidentSuccessResponse = null, incidentError = null) }
    }
}
