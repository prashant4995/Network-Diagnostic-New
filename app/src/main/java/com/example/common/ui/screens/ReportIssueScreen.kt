package com.example.common.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.common.domain.model.SupportTicket
import com.example.common.ui.SmartDiagnosticsViewModel
import com.example.common.ui.components.DiagnosticSnapshotView
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkNavyBackground
import com.example.ui.theme.DarkNavyBorder
import com.example.ui.theme.DarkNavySurface
import com.example.ui.theme.DarkNavySurfaceElevated
import com.example.ui.theme.OnPrimaryDark
import com.example.ui.theme.SkyBlue
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReportIssueScreen(
    viewModel: SmartDiagnosticsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedDevice by viewModel.selectedDevice.collectAsState()
    val currentNetwork by viewModel.currentNetwork.collectAsState()
    val latestReport by viewModel.latestReport.collectAsState()
    val tickets by viewModel.tickets.collectAsState()

    val categories = listOf(
        "Setup & Pairing Failure",
        "Intermittent Wi-Fi Disconnect",
        "Water Valve & Sensor Flow",
        "Voice & Smart Controller Lag",
        "Firmware Update Timeout"
    )

    var selectedCategory by remember { mutableStateOf(categories.first()) }
    var customerNotes by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var lastSubmittedTicket by remember { mutableStateOf<SupportTicket?>(null) }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val primaryIssue = latestReport?.issues?.firstOrNull()
    val currentErrorCode = primaryIssue?.errorCode ?: "NO_ACTIVE_FAULT"

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkNavyBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Bar
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DarkNavySurfaceElevated)
                        .border(1.dp, DarkNavyBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Column {
                    Text(
                        text = "Report Issue",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Device diagnostics attached automatically",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyanNeon
                    )
                }
            }
        }

        // Submission Success Card
        if (lastSubmittedTicket != null) {
            item {
                val ticket = lastSubmittedTicket!!
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkNavySurface)
                        .border(1.dp, StatusSuccess, RoundedCornerShape(16.dp))
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = StatusSuccess,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Ticket Dispatched to Engineering",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Text(
                            text = "Ticket ID: ${ticket.ticketId}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = CyanNeon
                        )
                        Text(
                            text = "Product: ${ticket.deviceName} (${ticket.deviceModel})",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "Error Captured: ${ticket.detectedErrorCode}",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusWarning,
                            fontWeight = FontWeight.SemiBold
                        )

                        Button(
                            onClick = {
                                val jsonDump = """
                                    {
                                      "ticketId": "${ticket.ticketId}",
                                      "product": "${ticket.deviceName}",
                                      "model": "${ticket.deviceModel}",
                                      "firmware": "${ticket.deviceFirmware}",
                                      "mac": "${ticket.deviceMac}",
                                      "category": "${ticket.issueCategory}",
                                      "errorCode": "${ticket.detectedErrorCode}",
                                      "failureReason": "${ticket.failureReason}",
                                      "wifi": {
                                        "ssid": "${ticket.networkSnapshot.ssid}",
                                        "frequencyBand": "${ticket.networkSnapshot.frequencyBand.label}",
                                        "rssiDbm": ${ticket.networkSnapshot.wifiSignalDbm},
                                        "security": "${ticket.networkSnapshot.securityType.label}",
                                        "ip": "${ticket.networkSnapshot.ipAddress}",
                                        "dhcpStatus": "${ticket.networkSnapshot.dhcpStatus.label}"
                                      },
                                      "ble": {
                                        "rssiDbm": ${ticket.networkSnapshot.bleSignalDbm},
                                        "state": "${ticket.networkSnapshot.bleState.label}"
                                      }
                                    }
                                """.trimIndent()
                                clipboardManager.setText(AnnotatedString(jsonDump))
                                Toast.makeText(context, "Copied Diagnostic JSON to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkNavySurfaceElevated,
                                contentColor = CyanNeon
                            ),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Copy Diagnostic JSON Payload", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Issue Category Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Select Issue Category",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { cat ->
                        val isSelected = cat == selectedCategory
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) DarkNavySurfaceElevated else DarkNavySurface)
                                .border(
                                    1.dp,
                                    if (isSelected) CyanNeon else DarkNavyBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = cat,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) CyanNeon else TextPrimary
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = CyanNeon,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // User notes field
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Customer Observations (Optional)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                OutlinedTextField(
                    value = customerNotes,
                    onValueChange = { customerNotes = it },
                    placeholder = {
                        Text(
                            text = "e.g. Shower digital valve lost connection when shifting to 5 GHz router...",
                            color = TextMuted
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .testTag("customer_note_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkNavySurface,
                        unfocusedContainerColor = DarkNavySurface,
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = DarkNavyBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        }

        // Auto-Enriched Diagnostic Snapshot
        item {
            DiagnosticSnapshotView(
                device = selectedDevice,
                network = currentNetwork,
                errorCode = currentErrorCode
            )
        }

        // Submit Button
        item {
            Button(
                onClick = {
                    isSubmitting = true
                    viewModel.onSubmitTicket(
                        category = selectedCategory,
                        note = customerNotes
                    ) { createdTicket ->
                        isSubmitting = false
                        lastSubmittedTicket = createdTicket
                        customerNotes = ""
                        Toast.makeText(context, "Escalation created: ${createdTicket.ticketId}", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanNeon,
                    contentColor = OnPrimaryDark
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_ticket_button")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = OnPrimaryDark,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Submit Ticket with Diagnostic Package",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Past Tickets History
        if (tickets.isNotEmpty()) {
            item {
                Text(
                    text = "Previous Escalations (${tickets.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            items(tickets) { ticket ->
                TicketHistoryItem(ticket = ticket)
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TicketHistoryItem(ticket: SupportTicket) {
    val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US).format(Date(ticket.timestamp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkNavySurface)
            .border(1.dp, DarkNavyBorder, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = ticket.ticketId,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = CyanGlow
            )
            Text(
                text = dateStr,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
        Text(
            text = "${ticket.deviceName} • ${ticket.issueCategory}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Fault Code: ${ticket.detectedErrorCode}",
            style = MaterialTheme.typography.labelSmall,
            color = StatusWarning
        )
    }
}
