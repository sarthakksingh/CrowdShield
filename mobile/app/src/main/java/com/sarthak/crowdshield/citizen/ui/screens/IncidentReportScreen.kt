package com.sarthak.crowdshield.citizen.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarthak.crowdshield.citizen.ui.CitizenUiState

@Composable
fun IncidentReportScreen(
    state: CitizenUiState,
    onSubmitIncident: (zoneId: String, category: String, severity: String, description: String) -> Unit,
    onResetStatus: () -> Unit
) {
    val defaultZones = listOf("z-corridor-1", "z-entry-a", "z-open-yard", "z-exit-east")
    val availableZones = if (state.zones.isNotEmpty()) state.zones.map { it.zoneId } else defaultZones

    var selectedZone by remember { mutableStateOf(availableZones.firstOrNull() ?: "z-corridor-1") }
    var selectedCategory by remember { mutableStateOf("CONGESTION") }
    var selectedSeverity by remember { mutableStateOf("HIGH") }
    var description by remember { mutableStateOf("") }

    var zoneDropdownExpanded by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var severityDropdownExpanded by remember { mutableStateOf(false) }

    val categories = listOf("CONGESTION", "OVERCROWDING", "BLOCKED_EXIT", "MEDICAL", "HAZARD")
    val severities = listOf("LOW", "MODERATE", "HIGH", "CRITICAL")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F19))
            .padding(16.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            item {
                Column {
                    Text(
                        text = "CITIZEN SAFETY DISPATCH",
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "Report Incident",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Submit real-time ground observations directly to venue operations triage.",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                }
            }

            // Success Card
            if (state.incidentSuccessResponse != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Success", tint = Color(0xFF34D399))
                                Text("Incident Report Received", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Incident ID: ${state.incidentSuccessResponse.incidentId}",
                                color = Color(0xFFA7F3D0),
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Status: ${state.incidentSuccessResponse.status} • Queue Position: ${state.incidentSuccessResponse.triageQueuePosition ?: 1}",
                                color = Color(0xFFD1FAE5),
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    description = ""
                                    onResetStatus()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                            ) {
                                Text("Submit Another Report")
                            }
                        }
                    }
                }
            }

            // Error Card
            if (state.incidentError != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Warning, contentDescription = "Error", tint = Color(0xFFFCA5A5))
                            Text(state.incidentError, color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            }

            // Main Form
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1. Zone Picker Dropdown
                        Column {
                            Text("SECTOR / LOCATION", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                    .clickable { zoneDropdownExpanded = true }
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(selectedZone, color = Color.White, fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select", tint = Color(0xFF94A3B8))
                                }
                                DropdownMenu(
                                    expanded = zoneDropdownExpanded,
                                    onDismissRequest = { zoneDropdownExpanded = false }
                                ) {
                                    availableZones.forEach { z ->
                                        DropdownMenuItem(
                                            text = { Text(z, fontFamily = FontFamily.Monospace) },
                                            onClick = {
                                                selectedZone = z
                                                zoneDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Category & Severity Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Category
                            Column(modifier = Modifier.weight(1f)) {
                                Text("CATEGORY", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                        .clickable { categoryDropdownExpanded = true }
                                        .padding(horizontal = 12.dp, vertical = 12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(selectedCategory, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                                    }
                                    DropdownMenu(
                                        expanded = categoryDropdownExpanded,
                                        onDismissRequest = { categoryDropdownExpanded = false }
                                    ) {
                                        categories.forEach { cat ->
                                            DropdownMenuItem(
                                                text = { Text(cat, fontSize = 13.sp) },
                                                onClick = {
                                                    selectedCategory = cat
                                                    categoryDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Severity
                            Column(modifier = Modifier.weight(1f)) {
                                Text("SEVERITY", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                        .clickable { severityDropdownExpanded = true }
                                        .padding(horizontal = 12.dp, vertical = 12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(selectedSeverity, color = getRiskColor(selectedSeverity), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                                    }
                                    DropdownMenu(
                                        expanded = severityDropdownExpanded,
                                        onDismissRequest = { severityDropdownExpanded = false }
                                    ) {
                                        severities.forEach { sev ->
                                            DropdownMenuItem(
                                                text = { Text(sev, color = getRiskColor(sev), fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                                                onClick = {
                                                    selectedSeverity = sev
                                                    severityDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 3. Description Field
                        Column {
                            Text("INCIDENT DETAILS", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                placeholder = { Text("e.g. Crowd movement stalled, turnstile jam at entrance...", color = Color(0xFF64748B), fontSize = 13.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF0F172A),
                                    unfocusedContainerColor = Color(0xFF0F172A),
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        // 4. Submit Button
                        Button(
                            onClick = {
                                onSubmitIncident(selectedZone, selectedCategory, selectedSeverity, description)
                            },
                            enabled = !state.isSubmittingIncident,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (state.isSubmittingIncident) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Transmitting...", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Submit", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Submit Safety Report", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
