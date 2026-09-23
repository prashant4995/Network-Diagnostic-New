package com.example.common.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.common.domain.model.ConnectedDevice
import com.example.common.domain.model.NetworkAttributes
import com.example.common.domain.model.WifiBand
import com.example.common.domain.model.WifiSecurity
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkNavyBorder
import com.example.ui.theme.DarkNavySurface
import com.example.ui.theme.DarkNavySurfaceElevated
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.SkyBlue
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun NetworkAttributeCard(
    device: ConnectedDevice,
    network: NetworkAttributes,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DarkNavySurface)
            .border(1.dp, DarkNavyBorder, RoundedCornerShape(18.dp))
            .padding(16.dp)
            .testTag("network_attribute_card"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyanNeon.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Router,
                        contentDescription = null,
                        tint = CyanNeon,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "1. In-App Diagnostics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Clear Visibility into Network Dependencies",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkNavySurfaceElevated)
                    .border(1.dp, DarkNavyBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = network.ssid,
                    style = MaterialTheme.typography.labelSmall,
                    color = SkyBlue,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Metrics Grid (2x2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Wi-Fi Signal Strength at product location
            AttributeTile(
                icon = Icons.Default.Wifi,
                title = "Wi-Fi Signal Strength",
                subtitle = "At product location",
                value = "${network.wifiSignalDbm} dBm",
                statusLabel = network.wifiSignalQuality,
                statusColor = when {
                    network.wifiSignalDbm >= -65 -> StatusSuccess
                    network.wifiSignalDbm >= -75 -> StatusWarning
                    else -> StatusError
                },
                modifier = Modifier.weight(1f)
            )

            // Bluetooth signal strength
            AttributeTile(
                icon = Icons.Default.Bluetooth,
                title = "Bluetooth Signal",
                subtitle = "BLE Proximity",
                value = "${network.bleSignalDbm} dBm",
                statusLabel = network.bleState.label,
                statusColor = when {
                    network.bleSignalDbm >= -70 -> StatusSuccess
                    network.bleSignalDbm >= -85 -> StatusWarning
                    else -> StatusError
                },
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Network attributes: 2.4 / 5 / 6 GHz
            val isBandOk = network.frequencyBand == WifiBand.BAND_2_4_GHZ
            AttributeTile(
                icon = Icons.Default.Router,
                title = "Frequency Band",
                subtitle = "Hardware requirement",
                value = network.frequencyBand.label,
                statusLabel = if (isBandOk) "Compatible (2.4G)" else "Mismatch (Req. 2.4G)",
                statusColor = if (isBandOk) StatusSuccess else StatusError,
                modifier = Modifier.weight(1f)
            )

            // Router Security & Mesh detection
            val isSecOk = network.securityType != WifiSecurity.WPA3_SAE
            AttributeTile(
                icon = Icons.Default.Lock,
                title = "Security & Mesh",
                subtitle = if (network.isMeshOrExtender) "Mesh / Extender Detected" else "Direct Router AP",
                value = when (network.securityType) {
                    WifiSecurity.WPA2_PSK -> "WPA2"
                    WifiSecurity.WPA3_SAE -> "WPA3 (SAE)"
                    WifiSecurity.WPA2_WPA3_MIXED -> "WPA2/WPA3"
                    else -> network.securityType.label
                },
                statusLabel = if (isSecOk) "Compatible" else "Unsupported WPA3",
                statusColor = if (isSecOk) StatusSuccess else StatusError,
                modifier = Modifier.weight(1f)
            )
        }

        // Sub-details: IP address, DHCP, Gateway
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(DarkNavySurfaceElevated)
                .border(1.dp, DarkNavyBorder, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lan,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "IP: ${network.ipAddress}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (network.ipAddress.startsWith("169.254")) StatusError else TextSecondary
                )
            }
            Text(
                text = "Gateway: ${network.gatewayIp}",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            Text(
                text = "Ping: ${if (network.cloudPingMs > 0) "${network.cloudPingMs}ms" else "Timeout"}",
                style = MaterialTheme.typography.labelSmall,
                color = if (network.cloudPingMs in 1..250) StatusSuccess else StatusError
            )
        }
    }
}

@Composable
private fun AttributeTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: String,
    statusLabel: String,
    statusColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(DarkNavySurfaceElevated)
            .border(1.dp, DarkNavyBorder, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CyanGlow,
                modifier = Modifier.size(18.dp)
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary
        )

        Text(
            text = statusLabel,
            style = MaterialTheme.typography.labelSmall,
            color = statusColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}
