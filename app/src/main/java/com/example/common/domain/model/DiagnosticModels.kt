package com.example.common.domain.model

enum class WifiBand(val label: String, val frequencyMhz: Int) {
    BAND_2_4_GHZ("2.4 GHz", 2412),
    BAND_5_GHZ("5 GHz", 5180),
    BAND_6_GHZ("6 GHz", 5955),
    DUAL_BAND_COMBINED("Dual-Band (Band Steering)", 2412)
}

enum class WifiSecurity(val label: String) {
    WPA2_PSK("WPA2-Personal (AES)"),
    WPA3_SAE("WPA3-Personal Only"),
    WPA2_WPA3_MIXED("WPA2/WPA3 Mixed (Recommended)"),
    OPEN("Open / Unsecured"),
    WEP("WEP (Legacy)")
}

enum class DhcpStatus(val label: String) {
    ASSIGNED("Valid IP Assigned"),
    TIMEOUT("DHCP Server Timeout"),
    APIPA_SELF_ASSIGNED("Self-Assigned (169.254.x.x)"),
    FAILED("Assignment Failed")
}

enum class BleState(val label: String) {
    CONNECTED("Connected & Paired"),
    ADVERTISING("Advertising Beacon"),
    OUT_OF_RANGE("Weak / Out of Range"),
    PAIRING_FAILED("Pairing Handshake Failed"),
    BLUETOOTH_OFF("Bluetooth Disabled")
}

enum class CheckStatus {
    PASS,
    WARNING,
    FAIL,
    IN_PROGRESS
}

enum class DiagnosticSeverity {
    CRITICAL,
    WARNING,
    OPTIMAL
}

data class ConnectedDevice(
    val id: String,
    val name: String,
    val room: String,
    val modelNumber: String,
    val serialNumber: String,
    val firmwareVersion: String,
    val macAddress: String,
    val requiredBand: String = "2.4 GHz (802.11 b/g/n)",
    val requiredSecurity: String = "WPA2-PSK or Mixed",
    val isOnline: Boolean = true
)

data class NetworkAttributes(
    val ssid: String,
    val bssid: String,
    val wifiSignalDbm: Int, // e.g. -55 dBm
    val frequencyBand: WifiBand,
    val securityType: WifiSecurity,
    val isMeshOrExtender: Boolean,
    val meshNodeName: String? = null,
    val ipAddress: String,
    val gatewayIp: String,
    val subnetMask: String = "255.255.255.0",
    val dnsServer: String = "1.1.1.1, 8.8.8.8",
    val dhcpStatus: DhcpStatus,
    val bleSignalDbm: Int, // e.g. -64 dBm
    val bleState: BleState,
    val cloudPingMs: Long = 38L, // ping to Kohler IoT Cloud
    val packetLossPercent: Int = 0,
    val isWifiEnabled: Boolean = true,
    val isBleEnabled: Boolean = true
) {
    val wifiSignalQuality: String
        get() = when {
            !isWifiEnabled || wifiSignalDbm <= -100 -> "Wi-Fi Off"
            wifiSignalDbm >= -65 -> "Excellent"
            wifiSignalDbm >= -75 -> "Fair"
            else -> "Poor / Weak"
        }

    val bleSignalQuality: String
        get() = when {
            !isBleEnabled || bleState == BleState.BLUETOOTH_OFF || bleSignalDbm <= -100 -> "Bluetooth Off"
            bleSignalDbm >= -70 -> "Strong"
            bleSignalDbm >= -85 -> "Acceptable"
            else -> "Very Weak"
        }
}

data class DiagnosticCheckItem(
    val id: String,
    val title: String,
    val category: String, // "Wi-Fi Signal", "Band Compatibility", "Router Security", "IP/DHCP", "BLE Link"
    val status: CheckStatus,
    val summary: String,
    val detail: String,
    val metricValue: String
)

data class IssueDiagnosis(
    val id: String,
    val title: String,
    val errorCode: String,
    val severity: DiagnosticSeverity,
    val specificFailureReason: String,
    val customerFriendlyExplanation: String,
    val guidedFixSteps: List<String>,
    val quickFixActionLabel: String? = null
)

data class SupportTicket(
    val ticketId: String,
    val timestamp: Long,
    val deviceName: String,
    val deviceModel: String,
    val deviceFirmware: String,
    val deviceMac: String,
    val issueCategory: String,
    val customerNote: String,
    val detectedErrorCode: String,
    val failureReason: String,
    val networkSnapshot: NetworkAttributes,
    val status: String = "Ticket Created & Dispatched"
)
