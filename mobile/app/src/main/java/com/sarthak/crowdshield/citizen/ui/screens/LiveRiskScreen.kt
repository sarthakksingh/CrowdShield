package com.sarthak.crowdshield.citizen.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarthak.crowdshield.citizen.data.model.OverallRiskDto
import com.sarthak.crowdshield.citizen.data.model.ZoneAnalyticsDto
import com.sarthak.crowdshield.citizen.ui.CitizenUiState

fun getRiskColor(level: String?): Color {
    return when (level?.uppercase()) {
        "CRITICAL" -> Color(0xFFEF4444)
        "HIGH" -> Color(0xFFF97316)
        "MODERATE" -> Color(0xFFEAB308)
        else -> Color(0xFF10B981)
    }
}

@Composable
fun LiveRiskScreen(
    state: CitizenUiState,
    onRefresh: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F19))
            .padding(16.dp)
    ) {
        if (state.isLiveRiskLoading && state.overallRisk == null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF38BDF8))
                Spacer(modifier = Modifier.height(12.dp))
                Text("Connecting to CrowdShield...", color = Color(0xFF94A3B8), fontSize = 14.sp)
            }
        } else if (state.liveRiskError != null && state.overallRisk == null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Error",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("Connection Error", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(state.liveRiskError, color = Color(0xFF94A3B8), fontSize = 13.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onRefresh) {
                    Text("Retry Connection")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Header Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "CROWDSHIELD",
                                color = Color(0xFF38BDF8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = "Live Venue Safety",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        IconButton(onClick = onRefresh) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                // Overall Risk Card
                item {
                    state.overallRisk?.let { risk ->
                        OverallRiskCard(risk = risk, lastRefreshedAt = state.lastRefreshedAt)
                    }
                }

                // Sector Breakdown Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SECTOR SAFETY METRICS",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${state.zones.size} Monitored Zones",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp
                        )
                    }
                }

                // Zone items
                items(state.zones, key = { it.zoneId }) { zone ->
                    ZoneItemCard(zone = zone)
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
fun OverallRiskCard(risk: OverallRiskDto, lastRefreshedAt: String) {
    val riskColor = getRiskColor(risk.level)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "OVERALL VENUE RISK",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .background(riskColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = risk.level,
                        color = riskColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = String.format("%.2f", risk.score),
                        color = riskColor,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Trend: ${risk.trend} • Conf: ${(risk.confidence * 100).toInt()}%",
                        color = Color(0xFFCBD5E1),
                        fontSize = 12.sp
                    )
                }

                risk.horizon?.let { horizon ->
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "TIME TO CRITICAL",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = horizon,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (lastRefreshedAt.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Live Sync: $lastRefreshedAt (updates every 3s)",
                    color = Color(0xFF64748B),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun ZoneItemCard(zone: ZoneAnalyticsDto) {
    val level = if (zone.bottleneck?.isBottleneck == true || zone.densityPerSqM >= 3.5) {
        "CRITICAL"
    } else if (zone.densityPerSqM >= 2.5 || zone.counterflow?.isCounterflowDetected == true) {
        "HIGH"
    } else if (zone.densityPerSqM >= 1.8) {
        "MODERATE"
    } else {
        "LOW"
    }
    val badgeColor = getRiskColor(level)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = zone.zoneId,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Box(
                    modifier = Modifier
                        .background(badgeColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = level,
                        color = badgeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Density", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    Text(
                        text = "${String.format("%.1f", zone.densityPerSqM)} /m²",
                        color = Color(0xFFE2E8F0),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column {
                    Text("Net Flow", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    Text(
                        text = "${if (zone.netPressure > 0) "+" else ""}${zone.netPressure}/m",
                        color = if (zone.netPressure > 0) Color(0xFFF97316) else Color(0xFF10B981),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column {
                    Text("Queue", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    Text(
                        text = "${zone.queueLength} pers",
                        color = Color(0xFFE2E8F0),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column {
                    Text("Speed Drop", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    Text(
                        text = "${zone.speedDropPct.toInt()}%",
                        color = if (zone.speedDropPct > 35) Color(0xFFEF4444) else Color(0xFFCBD5E1),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (zone.bottleneck?.isBottleneck == true) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFEF4444).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "⚠ Bottleneck Choke Detected",
                        color = Color(0xFFF87171),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (zone.counterflow?.isCounterflowDetected == true) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF97316).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "⚡ Counterflow Turbulence Detected",
                        color = Color(0xFFFB923C),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
