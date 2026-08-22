package com.sarthak.crowdshield.citizen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.sarthak.crowdshield.citizen.ui.CrowdShieldViewModel
import com.sarthak.crowdshield.citizen.ui.screens.AlertsScreen
import com.sarthak.crowdshield.citizen.ui.screens.IncidentReportScreen
import com.sarthak.crowdshield.citizen.ui.screens.LiveRiskScreen
import com.sarthak.crowdshield.citizen.ui.theme.CrowdShieldCitizenTheme

enum class Screen(val title: String, val icon: ImageVector) {
    LIVE_RISK("Live Risk", Icons.Default.Shield),
    ALERTS("Alerts", Icons.Default.Notifications),
    REPORT("Report", Icons.Default.AddAlert)
}

class MainActivity : ComponentActivity() {
    private val viewModel: CrowdShieldViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CrowdShieldCitizenTheme {
                MainApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainApp(viewModel: CrowdShieldViewModel) {
    val state by viewModel.uiState.collectAsState()
    var currentScreen by remember { mutableStateOf(Screen.LIVE_RISK) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF0B0F19),
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0F172A),
                contentColor = Color(0xFF94A3B8)
            ) {
                Screen.entries.forEach { screen ->
                    val selected = currentScreen == screen
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                color = if (selected) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF38BDF8),
                            unselectedIconColor = Color(0xFF64748B),
                            indicatorColor = Color(0xFF1E293B)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.LIVE_RISK -> LiveRiskScreen(
                    state = state,
                    onRefresh = { viewModel.fetchLiveRisk() }
                )
                Screen.ALERTS -> AlertsScreen(
                    state = state,
                    onRefresh = { viewModel.fetchAlerts() }
                )
                Screen.REPORT -> IncidentReportScreen(
                    state = state,
                    onSubmitIncident = { zoneId, category, severity, description ->
                        viewModel.submitIncident(zoneId, category, severity, description)
                    },
                    onResetStatus = { viewModel.clearIncidentStatus() }
                )
            }
        }
    }
}
