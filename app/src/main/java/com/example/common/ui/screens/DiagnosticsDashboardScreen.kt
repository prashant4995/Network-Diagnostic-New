package com.example.common.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.common.ui.SmartDiagnosticsViewModel
import com.example.common.ui.components.DiagnosticCheckRow
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkNavyBackground
import com.example.ui.theme.DarkNavyBorder
import com.example.ui.theme.DarkNavySurface
import com.example.ui.theme.DarkNavySurfaceElevated
import com.example.ui.theme.OnPrimaryDark
import com.example.ui.theme.SkyBlue
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class DashboardView {
    DASHBOARD,
    FIX_GUIDE,
    REPORT_ISSUE
}

@Composable
fun DiagnosticsDashboardScreen(
    viewModel: SmartDiagnosticsViewModel,
    onNavigateToFixGuide: () -> Unit = {},
    onNavigateToReport: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentView by remember { mutableStateOf(DashboardView.DASHBOARD) }

    when (currentView) {
        DashboardView.FIX_GUIDE -> {
            CustomerFixGuideScreen(
                viewModel = viewModel,
                onNavigateBack = { currentView = DashboardView.DASHBOARD },
                onNavigateToReport = { currentView = DashboardView.REPORT_ISSUE },
                modifier = modifier
            )
        }
        DashboardView.REPORT_ISSUE -> {
            ReportIssueScreen(
                viewModel = viewModel,
                onNavigateBack = { currentView = DashboardView.DASHBOARD },
                modifier = modifier
            )
        }
        DashboardView.DASHBOARD -> {
            DashboardMainContent(
                viewModel = viewModel,
                onNavigateToFixGuide = { currentView = DashboardView.FIX_GUIDE },
                onNavigateToReport = { currentView = DashboardView.REPORT_ISSUE },
                modifier = modifier
            )
        }
    }
}

@Composable
private fun DashboardMainContent(
    viewModel: SmartDiagnosticsViewModel,
    onNavigateToFixGuide: () -> Unit,
    onNavigateToReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val latestReport by viewModel.latestReport.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkNavyBackground)
            .padding(horizontal = 20.dp)
    ) {
        // Top Header
        Spacer(modifier = Modifier.height(8.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "KOHLER KONNECT",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = CyanNeon,
                letterSpacing = 1.8.sp
            )
            Text(
                text = "Smart Diagnostics",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Diagnostic Check Suite (Wi-Fi & BLE) Header with Scan Button positioned right next to it
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Wi-Fi & BLE Diagnostic Suite",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = if (isScanning) "Restarting diagnostic suite..." else "Live RF telemetry & pairing checks",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isScanning) CyanNeon else TextSecondary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { viewModel.onRunDiagnostics() },
                enabled = !isScanning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkNavySurfaceElevated,
                    contentColor = CyanNeon
                ),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.5f)),
                modifier = Modifier.testTag("scan_button")
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = CyanNeon,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Scan",
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isScanning) "Scanning..." else "Scan",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Suite list or restarting state
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isScanning) {
                item {
                    ScanningProgressCard()
                }
            } else {
                val items = latestReport?.checkItems ?: emptyList()
                if (items.isEmpty()) {
                    item {
                        EmptyScanPromptCard(onScan = { viewModel.onRunDiagnostics() })
                    }
                } else {
                    items(items) { item ->
                        DiagnosticCheckRow(item = item)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Action Buttons at Bottom: Fix Fast Guide & Report Issue
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onNavigateToFixGuide,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanNeon,
                    contentColor = OnPrimaryDark
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("fix_fast_guide_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Fix Fast Guide",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = onNavigateToReport,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = StatusError
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, StatusError.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("report_issue_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ReportProblem,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Report Issue",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ScanningProgressCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkNavySurface)
            .border(1.dp, CyanNeon.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = CyanNeon,
            trackColor = DarkNavySurfaceElevated
        )
        Text(
            text = "Restarting Diagnostic Suite...",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = CyanNeon
        )
        Text(
            text = "Probing 2.4 GHz channels, testing live RF signal levels, checking DHCP lease, and verifying Bluetooth pairing link...",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun EmptyScanPromptCard(onScan: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkNavySurface)
            .border(1.dp, DarkNavyBorder, RoundedCornerShape(14.dp))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Diagnostic Suite Ready",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Button(
            onClick = onScan,
            colors = ButtonDefaults.buttonColors(
                containerColor = DarkNavySurfaceElevated,
                contentColor = CyanNeon
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("Run Diagnostic Scan")
        }
    }
}
