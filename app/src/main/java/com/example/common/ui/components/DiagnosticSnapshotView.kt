package com.example.common.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.common.domain.model.ConnectedDevice
import com.example.common.domain.model.NetworkAttributes
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkNavyBorder
import com.example.ui.theme.DarkNavySurface
import com.example.ui.theme.DarkNavySurfaceElevated
import com.example.ui.theme.SkyBlue
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun DiagnosticSnapshotView(
    device: ConnectedDevice,
    network: NetworkAttributes,
    errorCode: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkNavySurface)
            .border(1.dp, DarkNavyBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AssignmentTurnedIn,
                    contentDescription = null,
                    tint = CyanNeon,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Enriched Diagnostic Snapshot",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(StatusSuccess.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Auto-Attached",
                    style = MaterialTheme.typography.labelSmall,
                    color = StatusSuccess,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Text(
            text = "Eliminates repetitive support triage questions by automatically bundling hardware state, RF metrics, and DHCP traces directly into the ticket.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )

        // Raw technical dump preview
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(DarkNavySurfaceElevated)
                .border(1.dp, DarkNavyBorder, RoundedCornerShape(10.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SnapshotRow(label = "DEVICE_NAME", value = device.name)
            SnapshotRow(label = "MODEL / S_N", value = "${device.modelNumber} / ${device.serialNumber}")
            SnapshotRow(label = "FIRMWARE_REV", value = device.firmwareVersion)
            SnapshotRow(label = "DEVICE_MAC", value = device.macAddress)
            SnapshotRow(label = "SSID / BSSID", value = "${network.ssid} (${network.bssid})")
            SnapshotRow(label = "BAND_FREQUENCY", value = "${network.frequencyBand.label} (${network.frequencyBand.frequencyMhz} MHz)")
            SnapshotRow(label = "WIFI_RSSI", value = "${network.wifiSignalDbm} dBm (Quality: ${network.wifiSignalQuality})")
            SnapshotRow(label = "SECURITY_TYPE", value = network.securityType.label)
            SnapshotRow(label = "IP / GATEWAY", value = "${network.ipAddress} / ${network.gatewayIp}")
            SnapshotRow(label = "DHCP_STATE", value = network.dhcpStatus.label)
            SnapshotRow(label = "BLE_RSSI", value = "${network.bleSignalDbm} dBm (${network.bleState.label})")
            SnapshotRow(label = "ERROR_CODE", value = errorCode, isHighlight = true)
        }
    }
}

@Composable
private fun SnapshotRow(
    label: String,
    value: String,
    isHighlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = TextMuted,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = if (isHighlight) CyanNeon else TextPrimary,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}
