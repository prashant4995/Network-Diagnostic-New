package com.example.common.domain.engine

import com.example.common.domain.model.BleState
import com.example.common.domain.model.CheckStatus
import com.example.common.domain.model.ConnectedDevice
import com.example.common.domain.model.DhcpStatus
import com.example.common.domain.model.DiagnosticCheckItem
import com.example.common.domain.model.DiagnosticSeverity
import com.example.common.domain.model.IssueDiagnosis
import com.example.common.domain.model.NetworkAttributes
import com.example.common.domain.model.WifiBand
import com.example.common.domain.model.WifiSecurity

/**
 * Smart Diagnostics Engine for Kohler Konnect Smart Connected Products.
 * Evaluates in-app telemetry (Wi-Fi, BLE, Security, DHCP, Band, Ping) and
 * produces actionable root causes, customer-friendly explanations, and guided resolutions.
 */
class SmartDiagnosticsEngine {

    fun runDiagnostics(
        device: ConnectedDevice,
        network: NetworkAttributes
    ): DiagnosticReport {
        val checkItems = mutableListOf<DiagnosticCheckItem>()
        val issues = mutableListOf<IssueDiagnosis>()

        val isWifiOff = !network.isWifiEnabled || network.wifiSignalDbm <= -100 || network.ssid == "Wi-Fi Disabled"
        val isBleOff = !network.isBleEnabled || network.bleState == BleState.BLUETOOTH_OFF || network.bleSignalDbm <= -100

        // 1. Wi-Fi Band Compatibility Check
        val bandCheck = evaluateBand(network)
        checkItems.add(bandCheck)

        if (isWifiOff) {
            issues.add(
                IssueDiagnosis(
                    id = "ISSUE_WIFI_DISABLED",
                    title = "Device Wi-Fi is Turned Off",
                    errorCode = "ERR_WIFI_DISABLED",
                    severity = DiagnosticSeverity.CRITICAL,
                    specificFailureReason = "Device Wi-Fi radio is disabled. Setup, telemetry streaming, and cloud control require an active Wi-Fi connection.",
                    customerFriendlyExplanation = "Your phone's Wi-Fi is currently turned off. Turn on Wi-Fi in your device settings to communicate with your smart fixture.",
                    guidedFixSteps = listOf(
                        "Swipe down from the top of your screen to open Quick Settings.",
                        "Tap the Wi-Fi icon to turn Wi-Fi ON.",
                        "Connect to your home 2.4 GHz Wi-Fi network.",
                        "Return to Kohler Konnect and tap 'Scan' next to the diagnostic suite to re-test."
                    ),
                    quickFixActionLabel = "Turn On Device Wi-Fi"
                )
            )
        } else if (bandCheck.status == CheckStatus.FAIL) {
            issues.add(
                IssueDiagnosis(
                    id = "ISSUE_BAND_MISMATCH",
                    title = "Unsupported Wi-Fi Band",
                    errorCode = "ERR_WIFI_5GHZ_BAND_MISMATCH",
                    severity = DiagnosticSeverity.CRITICAL,
                    specificFailureReason = "Router is operating in ${network.frequencyBand.label} mode only. Kohler smart device (${device.name}) hardware radio operates exclusively on 2.4 GHz (802.11 b/g/n).",
                    customerFriendlyExplanation = "Your router is using 5 GHz only. This product requires a 2.4 GHz network.",
                    guidedFixSteps = listOf(
                        "Open your Wi-Fi router app or admin settings page (usually 192.168.1.1 or 192.168.0.1).",
                        "Look for Wireless Settings or Dual-Band options.",
                        "Enable the 2.4 GHz Wi-Fi radio and ensure it broadcasts a separate name or split SSID.",
                        "Connect your smartphone to the 2.4 GHz Wi-Fi network and tap 'Re-run Diagnostic'."
                    ),
                    quickFixActionLabel = "Separate 2.4GHz Network"
                )
            )
        } else if (bandCheck.status == CheckStatus.WARNING) {
            issues.add(
                IssueDiagnosis(
                    id = "ISSUE_BAND_STEERING",
                    title = "Band Steering Conflict Detected",
                    errorCode = "WARN_WIFI_BAND_STEERING",
                    severity = DiagnosticSeverity.WARNING,
                    specificFailureReason = "Combined dual-band SSID detected. Mesh router is attempting to steer IoT client to 5 GHz, causing intermittent drops.",
                    customerFriendlyExplanation = "Your router combines 2.4 GHz and 5 GHz under one name. Some smart products struggle when the router shifts bands.",
                    guidedFixSteps = listOf(
                        "In your router app (e.g. Eero, Orbi, Nest Wifi), create a dedicated 'IoT Network' or 'Guest 2.4 GHz Network'.",
                        "Pair your Kohler device to the dedicated 2.4 GHz network."
                    ),
                    quickFixActionLabel = "Enable IoT 2.4GHz Guest Band"
                )
            )
        }

        // 2. Wi-Fi Signal Strength Check
        val signalCheck = evaluateSignalStrength(network)
        checkItems.add(signalCheck)
        if (!isWifiOff && signalCheck.status == CheckStatus.FAIL) {
            issues.add(
                IssueDiagnosis(
                    id = "ISSUE_WEAK_SIGNAL",
                    title = "Weak Signal at Location",
                    errorCode = "ERR_WEAK_SIGNAL_AT_LOCATION",
                    severity = DiagnosticSeverity.CRITICAL,
                    specificFailureReason = "Received Signal Strength Indicator (RSSI) is ${network.wifiSignalDbm} dBm (below minimum threshold of -75 dBm). Packet loss is ${network.packetLossPercent}%.",
                    customerFriendlyExplanation = "The Wi-Fi signal at your ${device.room} is too weak for the smart controller to stay reliably connected.",
                    guidedFixSteps = listOf(
                        "Ensure large metal fixtures, bathroom mirrors, or tiled walls aren't directly blocking the line of sight to your router.",
                        "Consider placing a Wi-Fi mesh node or extender within 15–20 feet of the ${device.room}.",
                        "If using an extender, ensure the extender is connected to your primary 2.4 GHz network."
                    ),
                    quickFixActionLabel = "Optimize Router / Extender Position"
                )
            )
        }

        // 3. Router Security Protocol Check
        val securityCheck = evaluateSecurity(network)
        checkItems.add(securityCheck)
        if (!isWifiOff && securityCheck.status == CheckStatus.FAIL) {
            issues.add(
                IssueDiagnosis(
                    id = "ISSUE_SECURITY_MISMATCH",
                    title = "Router Security Mismatch",
                    errorCode = "ERR_ROUTER_SECURITY_MISMATCH",
                    severity = DiagnosticSeverity.CRITICAL,
                    specificFailureReason = "Router encryption is set to ${network.securityType.label}. IoT hardware radio supports WPA2-Personal (AES) or WPA2/WPA3 Mixed mode.",
                    customerFriendlyExplanation = "Your router is configured to WPA3-only security. This product requires WPA2 or WPA2/WPA3 Mixed mode.",
                    guidedFixSteps = listOf(
                        "Log into your router configuration console.",
                        "Navigate to Security / Wireless Encryption settings.",
                        "Change encryption from 'WPA3-Personal Only' to 'WPA2/WPA3 Mixed (Transitional)'.",
                        "Save and reboot your router, then re-test connection."
                    ),
                    quickFixActionLabel = "Switch to WPA2/WPA3 Mixed"
                )
            )
        }

        // 4. IP / DHCP Assignment Check
        val dhcpCheck = evaluateDhcp(network)
        checkItems.add(dhcpCheck)
        if (!isWifiOff && dhcpCheck.status == CheckStatus.FAIL) {
            issues.add(
                IssueDiagnosis(
                    id = "ISSUE_DHCP_FAILURE",
                    title = "IP / DHCP Assignment Failure",
                    errorCode = "ERR_IP_DHCP_ASSIGNMENT_FAILURE",
                    severity = DiagnosticSeverity.CRITICAL,
                    specificFailureReason = "Device received APIPA fallback address ${network.ipAddress} (DHCP status: ${network.dhcpStatus.label}). Default gateway ${network.gatewayIp} is unreachable.",
                    customerFriendlyExplanation = "Your router connected to the device radio but failed to give it a valid local IP address.",
                    guidedFixSteps = listOf(
                        "Check if your router's DHCP client pool has run out of available IP addresses.",
                        "Power cycle your router by unplugging it for 30 seconds and plugging it back in.",
                        "Restart the Kohler ${device.name} controller power module."
                    ),
                    quickFixActionLabel = "Reboot Router & Refresh DHCP"
                )
            )
        }

        // 5. Bluetooth Signal & Proximity Check
        val bleCheck = evaluateBle(network)
        checkItems.add(bleCheck)
        if (isBleOff) {
            issues.add(
                IssueDiagnosis(
                    id = "ISSUE_BLE_DISABLED",
                    title = "Device Bluetooth is Turned Off",
                    errorCode = "ERR_BLUETOOTH_DISABLED",
                    severity = DiagnosticSeverity.CRITICAL,
                    specificFailureReason = "Bluetooth adapter is turned off on this mobile device. Direct setup, local pairing, and firmware handshakes require active Bluetooth.",
                    customerFriendlyExplanation = "Your phone's Bluetooth is currently turned off. Turn on Bluetooth in device settings to detect and pair your smart Kohler fixture.",
                    guidedFixSteps = listOf(
                        "Swipe down from the top of your screen to open Quick Settings.",
                        "Tap the Bluetooth icon to turn Bluetooth ON.",
                        "Make sure your smart Kohler product is within range (under 15 feet).",
                        "Return to Kohler Konnect and tap 'Scan' next to the diagnostic suite to re-test."
                    ),
                    quickFixActionLabel = "Turn On Bluetooth"
                )
            )
        } else if (bleCheck.status == CheckStatus.FAIL) {
            issues.add(
                IssueDiagnosis(
                    id = "ISSUE_BLE_DISCONNECTED",
                    title = "Bluetooth Pairing Disconnected",
                    errorCode = "WARN_BLE_PROXIMITY_DROP",
                    severity = DiagnosticSeverity.WARNING,
                    specificFailureReason = "BLE signal is ${network.bleSignalDbm} dBm (${network.bleState.label}). Direct setup and localized commands require active BLE handshake.",
                    customerFriendlyExplanation = "Your phone is too far from the ${device.name} or Bluetooth pairing timed out.",
                    guidedFixSteps = listOf(
                        "Move within 6 feet of your ${device.name}.",
                        "Ensure phone Bluetooth is toggled ON.",
                        "Put the Kohler controller in pairing mode by holding the setup button for 3 seconds."
                    ),
                    quickFixActionLabel = "Move Closer & Retry BLE"
                )
            )
        }

        // 6. Cloud & Gateway Reachability Check
        val cloudCheck = evaluateCloud(network)
        checkItems.add(cloudCheck)
        if (cloudCheck.status == CheckStatus.FAIL && issues.none { it.errorCode == "ERR_IP_DHCP_ASSIGNMENT_FAILURE" }) {
            issues.add(
                IssueDiagnosis(
                    id = "ISSUE_CLOUD_UNREACHABLE",
                    title = "Kohler Cloud Reachability Failure",
                    errorCode = "ERR_CLOUD_TELEMETRY_BLOCKED",
                    severity = DiagnosticSeverity.CRITICAL,
                    specificFailureReason = "Local gateway answered, but outbound TLS telemetry to Kohler IoT Cloud timed out (Ping: ${network.cloudPingMs} ms). Potential firewall or captive portal blocking port 8883/443.",
                    customerFriendlyExplanation = "Your network is blocking communication with Kohler Cloud services.",
                    guidedFixSteps = listOf(
                        "If you are on a guest network, accept the captive portal login page on your browser.",
                        "Ensure your router firewall is not blocking outbound MQTT/TLS traffic (ports 443 & 8883).",
                        "Verify your home Internet provider has an active upstream connection."
                    ),
                    quickFixActionLabel = "Inspect Router Firewall"
                )
            )
        }

        val overallSeverity = when {
            issues.any { it.severity == DiagnosticSeverity.CRITICAL } -> DiagnosticSeverity.CRITICAL
            issues.any { it.severity == DiagnosticSeverity.WARNING } -> DiagnosticSeverity.WARNING
            else -> DiagnosticSeverity.OPTIMAL
        }

        return DiagnosticReport(
            timestamp = System.currentTimeMillis(),
            device = device,
            network = network,
            overallSeverity = overallSeverity,
            checkItems = checkItems,
            issues = issues
        )
    }

    private fun evaluateBand(network: NetworkAttributes): DiagnosticCheckItem {
        if (!network.isWifiEnabled || network.wifiSignalDbm <= -100 || network.ssid == "Wi-Fi Disabled") {
            return DiagnosticCheckItem(
                id = "CHECK_BAND",
                title = "Wi-Fi Frequency Band",
                category = "Network Attributes",
                status = CheckStatus.FAIL,
                summary = "Wi-Fi Radio Disabled",
                detail = "Wi-Fi is turned off on this mobile device. Enable Wi-Fi to scan 2.4 GHz network.",
                metricValue = "Wi-Fi Off"
            )
        }
        return when (network.frequencyBand) {
            WifiBand.BAND_5_GHZ, WifiBand.BAND_6_GHZ -> DiagnosticCheckItem(
                id = "CHECK_BAND",
                title = "Wi-Fi Frequency Band",
                category = "Network Attributes",
                status = CheckStatus.FAIL,
                summary = "Unsupported Band (${network.frequencyBand.label})",
                detail = "Connected to ${network.frequencyBand.label}. Smart product requires 2.4 GHz.",
                metricValue = network.frequencyBand.label
            )
            WifiBand.DUAL_BAND_COMBINED -> DiagnosticCheckItem(
                id = "CHECK_BAND",
                title = "Wi-Fi Frequency Band",
                category = "Network Attributes",
                status = CheckStatus.WARNING,
                summary = "Combined Dual-Band (Steering)",
                detail = "2.4 GHz present, but combined SSID may trigger steering drops.",
                metricValue = "2.4 / 5 GHz Combined"
            )
            WifiBand.BAND_2_4_GHZ -> DiagnosticCheckItem(
                id = "CHECK_BAND",
                title = "Wi-Fi Frequency Band",
                category = "Network Attributes",
                status = CheckStatus.PASS,
                summary = "Optimal 2.4 GHz Band",
                detail = "Device hardware fully compatible with active 2.4 GHz channel.",
                metricValue = "2.4 GHz (Channel 6)"
            )
        }
    }

    private fun evaluateSignalStrength(network: NetworkAttributes): DiagnosticCheckItem {
        if (!network.isWifiEnabled || network.wifiSignalDbm <= -100 || network.ssid == "Wi-Fi Disabled") {
            return DiagnosticCheckItem(
                id = "CHECK_SIGNAL",
                title = "Wi-Fi Signal Strength",
                category = "In-App Diagnostics",
                status = CheckStatus.FAIL,
                summary = "Wi-Fi Radio Disabled (No Signal)",
                detail = "Wi-Fi is turned off. Turn on Wi-Fi in settings to establish an RF signal link.",
                metricValue = "0 dBm (Off)"
            )
        }
        return when {
            network.wifiSignalDbm >= -67 -> DiagnosticCheckItem(
                id = "CHECK_SIGNAL",
                title = "Wi-Fi Signal Strength",
                category = "In-App Diagnostics",
                status = CheckStatus.PASS,
                summary = "Strong Signal (${network.wifiSignalDbm} dBm)",
                detail = "Excellent signal propagation at product location. Zero packet drop.",
                metricValue = "${network.wifiSignalDbm} dBm (Good)"
            )
            network.wifiSignalDbm >= -75 -> DiagnosticCheckItem(
                id = "CHECK_SIGNAL",
                title = "Wi-Fi Signal Strength",
                category = "In-App Diagnostics",
                status = CheckStatus.WARNING,
                summary = "Marginal Signal (${network.wifiSignalDbm} dBm)",
                detail = "Signal is adequate, but environmental interference may cause sporadic delays.",
                metricValue = "${network.wifiSignalDbm} dBm (Fair)"
            )
            else -> DiagnosticCheckItem(
                id = "CHECK_SIGNAL",
                title = "Wi-Fi Signal Strength",
                category = "In-App Diagnostics",
                status = CheckStatus.FAIL,
                summary = "Weak Signal at Location (${network.wifiSignalDbm} dBm)",
                detail = "Signal attenuation is too high. Minimum recommended is -70 dBm.",
                metricValue = "${network.wifiSignalDbm} dBm (Critical)"
            )
        }
    }

    private fun evaluateSecurity(network: NetworkAttributes): DiagnosticCheckItem {
        if (!network.isWifiEnabled || network.wifiSignalDbm <= -100 || network.ssid == "Wi-Fi Disabled") {
            return DiagnosticCheckItem(
                id = "CHECK_SECURITY",
                title = "Router Security Protocol",
                category = "Network Attributes",
                status = CheckStatus.FAIL,
                summary = "Wi-Fi Disconnected",
                detail = "Security handshake cannot be verified while Wi-Fi is disabled.",
                metricValue = "Disconnected"
            )
        }
        return when (network.securityType) {
            WifiSecurity.WPA3_SAE -> DiagnosticCheckItem(
                id = "CHECK_SECURITY",
                title = "Router Security Protocol",
                category = "Network Attributes",
                status = CheckStatus.FAIL,
                summary = "Security Mismatch (WPA3 Only)",
                detail = "IoT hardware requires WPA2-PSK or WPA2/WPA3 Mixed mode.",
                metricValue = network.securityType.label
            )
            WifiSecurity.OPEN, WifiSecurity.WEP -> DiagnosticCheckItem(
                id = "CHECK_SECURITY",
                title = "Router Security Protocol",
                category = "Network Attributes",
                status = CheckStatus.WARNING,
                summary = "Insecure Network (${network.securityType.label})",
                detail = "Smart home device security requires encrypted WPA2/WPA3 network.",
                metricValue = network.securityType.label
            )
            else -> DiagnosticCheckItem(
                id = "CHECK_SECURITY",
                title = "Router Security Protocol",
                category = "Network Attributes",
                status = CheckStatus.PASS,
                summary = "Compatible Encryption",
                detail = "Using ${network.securityType.label}, fully supported by smart module.",
                metricValue = network.securityType.label
            )
        }
    }

    private fun evaluateDhcp(network: NetworkAttributes): DiagnosticCheckItem {
        if (!network.isWifiEnabled || network.wifiSignalDbm <= -100 || network.ssid == "Wi-Fi Disabled") {
            return DiagnosticCheckItem(
                id = "CHECK_DHCP",
                title = "IP / DHCP Assignment",
                category = "In-App Diagnostics",
                status = CheckStatus.FAIL,
                summary = "No Network Connection",
                detail = "Cannot assign local IP address or gateway while Wi-Fi is disabled.",
                metricValue = "Disconnected"
            )
        }
        return when (network.dhcpStatus) {
            DhcpStatus.ASSIGNED -> DiagnosticCheckItem(
                id = "CHECK_DHCP",
                title = "IP / DHCP Assignment",
                category = "In-App Diagnostics",
                status = CheckStatus.PASS,
                summary = "Valid IP Assigned",
                detail = "Assigned IP: ${network.ipAddress} via Gateway ${network.gatewayIp}.",
                metricValue = network.ipAddress
            )
            DhcpStatus.APIPA_SELF_ASSIGNED -> DiagnosticCheckItem(
                id = "CHECK_DHCP",
                title = "IP / DHCP Assignment",
                category = "In-App Diagnostics",
                status = CheckStatus.FAIL,
                summary = "Self-Assigned APIPA (169.254.x.x)",
                detail = "Router DHCP server failed to lease an IP address to the controller.",
                metricValue = "${network.ipAddress} (APIPA)"
            )
            else -> DiagnosticCheckItem(
                id = "CHECK_DHCP",
                title = "IP / DHCP Assignment",
                category = "In-App Diagnostics",
                status = CheckStatus.FAIL,
                summary = "DHCP Lease Timeout",
                detail = "Controller requested IP address but received no DHCP ACK packet.",
                metricValue = "DHCP Timeout"
            )
        }
    }

    private fun evaluateBle(network: NetworkAttributes): DiagnosticCheckItem {
        if (!network.isBleEnabled || network.bleState == BleState.BLUETOOTH_OFF || network.bleSignalDbm <= -100) {
            return DiagnosticCheckItem(
                id = "CHECK_BLE",
                title = "Bluetooth Signal & Link",
                category = "In-App Diagnostics",
                status = CheckStatus.FAIL,
                summary = "Bluetooth Radio Disabled",
                detail = "Bluetooth is turned off on this mobile device. Direct pairing requires active Bluetooth.",
                metricValue = "Bluetooth Off"
            )
        }
        return when (network.bleState) {
            BleState.CONNECTED -> DiagnosticCheckItem(
                id = "CHECK_BLE",
                title = "Bluetooth Signal & Link",
                category = "In-App Diagnostics",
                status = CheckStatus.PASS,
                summary = "BLE Connected (${network.bleSignalDbm} dBm)",
                detail = "Direct mobile-to-product Bluetooth channel is active and responsive.",
                metricValue = "${network.bleSignalDbm} dBm (Paired)"
            )
            BleState.ADVERTISING -> DiagnosticCheckItem(
                id = "CHECK_BLE",
                title = "Bluetooth Signal & Link",
                category = "In-App Diagnostics",
                status = CheckStatus.WARNING,
                summary = "Beacon Detected (${network.bleSignalDbm} dBm)",
                detail = "Product is advertising pairing beacon, ready for secure handshake.",
                metricValue = "Ready to Pair"
            )
            else -> DiagnosticCheckItem(
                id = "CHECK_BLE",
                title = "Bluetooth Signal & Link",
                category = "In-App Diagnostics",
                status = CheckStatus.FAIL,
                summary = "BLE Disconnected / Out of Range",
                detail = "Unable to establish low-energy Bluetooth channel with smart fixture.",
                metricValue = "Disconnected"
            )
        }
    }

    private fun evaluateCloud(network: NetworkAttributes): DiagnosticCheckItem {
        if (!network.isWifiEnabled || network.wifiSignalDbm <= -100 || network.ssid == "Wi-Fi Disabled") {
            return DiagnosticCheckItem(
                id = "CHECK_CLOUD",
                title = "Kohler Cloud Telemetry",
                category = "Cloud Visibility",
                status = CheckStatus.FAIL,
                summary = "Cloud Unreachable (Wi-Fi Off)",
                detail = "Cannot reach Kohler IoT Cloud because device Wi-Fi is disconnected.",
                metricValue = "Offline"
            )
        }
        return if (network.cloudPingMs in 1..250) {
            DiagnosticCheckItem(
                id = "CHECK_CLOUD",
                title = "Kohler Cloud Telemetry",
                category = "Cloud Visibility",
                status = CheckStatus.PASS,
                summary = "Cloud Reachable (${network.cloudPingMs} ms)",
                detail = "Connected to Kohler IoT Broker (MQTT port 8883 confirmed).",
                metricValue = "${network.cloudPingMs} ms RTT"
            )
        } else if (network.cloudPingMs in 251..600) {
            DiagnosticCheckItem(
                id = "CHECK_CLOUD",
                title = "Kohler Cloud Telemetry",
                category = "Cloud Visibility",
                status = CheckStatus.WARNING,
                summary = "High Latency (${network.cloudPingMs} ms)",
                detail = "Telemetry packets are delayed. May cause lag in voice control and app updates.",
                metricValue = "${network.cloudPingMs} ms"
            )
        } else {
            DiagnosticCheckItem(
                id = "CHECK_CLOUD",
                title = "Kohler Cloud Telemetry",
                category = "Cloud Visibility",
                status = CheckStatus.FAIL,
                summary = "Cloud Unreachable",
                detail = "Ping to IoT endpoint timed out. Outbound MQTT traffic may be blocked.",
                metricValue = "Unreachable"
            )
        }
    }
}

data class DiagnosticReport(
    val timestamp: Long,
    val device: ConnectedDevice,
    val network: NetworkAttributes,
    val overallSeverity: DiagnosticSeverity,
    val checkItems: List<DiagnosticCheckItem>,
    val issues: List<IssueDiagnosis>
)
