package com.example.common.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.common.domain.model.CheckStatus
import com.example.common.domain.model.DiagnosticCheckItem
import com.example.common.domain.model.DiagnosticSeverity
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkNavyBackground
import com.example.ui.theme.DarkNavyBorder
import com.example.ui.theme.DarkNavySurface
import com.example.ui.theme.SkyBlue
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DiagnosticRadarView(
    isScanning: Boolean,
    severity: DiagnosticSeverity,
    checkItems: List<DiagnosticCheckItem>,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarTransitions")

    // Ripple expansion
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseProgress"
    )

    // Rotating radar beam
    val radarAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarRotation"
    )

    val centerColor = when (severity) {
        DiagnosticSeverity.CRITICAL -> StatusError
        DiagnosticSeverity.WARNING -> StatusWarning
        DiagnosticSeverity.OPTIMAL -> CyanNeon
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        DarkNavySurface,
                        DarkNavyBackground
                    )
                )
            )
            .border(1.dp, DarkNavyBorder, RoundedCornerShape(20.dp))
            .testTag("diagnostic_radar_canvas"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = minOf(size.width, size.height) * 0.44f

            // Concentric radar guide circles
            for (i in 1..3) {
                val r = maxRadius * (i / 3f)
                drawCircle(
                    color = DarkNavyBorder.copy(alpha = 0.6f),
                    radius = r,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // Radar crosshair axes
            drawLine(
                color = DarkNavyBorder.copy(alpha = 0.4f),
                start = Offset(center.x - maxRadius, center.y),
                end = Offset(center.x + maxRadius, center.y),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = DarkNavyBorder.copy(alpha = 0.4f),
                start = Offset(center.x, center.y - maxRadius),
                end = Offset(center.x, center.y + maxRadius),
                strokeWidth = 1.dp.toPx()
            )

            // Animated scanning pulse ring
            if (isScanning || severity != DiagnosticSeverity.OPTIMAL) {
                val pulseRadius = maxRadius * pulseProgress
                drawCircle(
                    color = centerColor.copy(alpha = (1f - pulseProgress) * 0.45f),
                    radius = pulseRadius,
                    center = center,
                    style = Stroke(width = 2.5.dp.toPx())
                )
            }

            // Sweep radar beam line
            val rad = Math.toRadians(radarAngle.toDouble())
            val beamEnd = Offset(
                x = center.x + (maxRadius * cos(rad)).toFloat(),
                y = center.y + (maxRadius * sin(rad)).toFloat()
            )
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(centerColor.copy(alpha = 0.8f), Color.Transparent),
                    start = center,
                    end = beamEnd
                ),
                start = center,
                end = beamEnd,
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Center Wi-Fi Hub Glow
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            centerColor.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                )
                .border(2.dp, centerColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Wifi,
                contentDescription = "Wi-Fi Hub Icon",
                tint = centerColor,
                modifier = Modifier.size(34.dp)
            )
        }

        // Top-left status badge
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkNavySurface.copy(alpha = 0.9f))
                .border(1.dp, DarkNavyBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isScanning) SkyBlue else centerColor)
                )
                Text(
                    text = if (isScanning) "SCANNING STACK..." else when (severity) {
                        DiagnosticSeverity.CRITICAL -> "ISSUE DETECTED"
                        DiagnosticSeverity.WARNING -> "WARNING DETECTED"
                        DiagnosticSeverity.OPTIMAL -> "SYSTEM HEALTHY"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            }
        }

        // Top-right Kohler badge
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(14.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkNavySurface.copy(alpha = 0.9f))
                .border(1.dp, DarkNavyBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = "KOHLER KONNECT",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = CyanGlow,
                letterSpacing = 1.2.sp
            )
        }

        // Bottom Probe Indicators Summary
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(DarkNavySurface.copy(alpha = 0.85f))
                .border(1.dp, DarkNavyBorder, RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val passCount = checkItems.count { it.status == CheckStatus.PASS }
            val failCount = checkItems.count { it.status == CheckStatus.FAIL }
            val warnCount = checkItems.count { it.status == CheckStatus.WARNING }

            MiniStatusPill(
                icon = Icons.Default.CheckCircle,
                count = "$passCount Passed",
                color = StatusSuccess
            )
            if (warnCount > 0) {
                MiniStatusPill(
                    icon = Icons.Default.Warning,
                    count = "$warnCount Warning",
                    color = StatusWarning
                )
            }
            if (failCount > 0) {
                MiniStatusPill(
                    icon = Icons.Default.Error,
                    count = "$failCount Fault",
                    color = StatusError
                )
            }
        }
    }
}

@Composable
private fun MiniStatusPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = count,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}
