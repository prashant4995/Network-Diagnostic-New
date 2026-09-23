package com.example.common.data

import com.example.common.data.probe.NetworkProbe
import com.example.common.domain.engine.DiagnosticReport
import com.example.common.domain.engine.SmartDiagnosticsEngine
import com.example.common.domain.model.BleState
import com.example.common.domain.model.CheckStatus
import com.example.common.domain.model.ConnectedDevice
import com.example.common.domain.model.DhcpStatus
import com.example.common.domain.model.NetworkAttributes
import com.example.common.domain.model.SupportTicket
import com.example.common.domain.model.WifiBand
import com.example.common.domain.model.WifiSecurity
import com.example.common.domain.repository.DiagnosticsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InMemoryDiagnosticsRepository(
    private val networkProbe: NetworkProbe,
    private val engine: SmartDiagnosticsEngine = SmartDiagnosticsEngine(),
    private val enableLiveMonitoring: Boolean = true
) : DiagnosticsRepository {

    private val defaultDevices = listOf(
        ConnectedDevice(
            id = "DEV_DTV_PLUS",
            name = "DTV+ Digital Shower System",
            room = "Master Bathroom",
            modelNumber = "K-99695-NA",
            serialNumber = "KHL-2024-99812",
            firmwareVersion = "v3.14.2",
            macAddress = "3C:71:BF:88:21:4A"
        ),
        ConnectedDevice(
            id = "DEV_PURIST_FAUCET",
            name = "Purist Touchless Smart Faucet",
            room = "Kitchen Island",
            modelNumber = "K-22033-NA",
            serialNumber = "KHL-2024-44109",
            firmwareVersion = "v2.8.0",
            macAddress = "3C:71:BF:41:99:BC"
        ),
        ConnectedDevice(
            id = "DEV_NUMI_TOILET",
            name = "Numi 2.0 Intelligent Toilet",
            room = "Powder Room",
            modelNumber = "K-30754-PA",
            serialNumber = "KHL-2025-00124",
            firmwareVersion = "v4.0.1",
            macAddress = "70:B3:D5:12:34:56"
        ),
        ConnectedDevice(
            id = "DEV_VERDERA_MIRROR",
            name = "Verdera Voice Smart Mirror",
            room = "Guest En-Suite",
            modelNumber = "K-99571-V-NA",
            serialNumber = "KHL-2023-88741",
            firmwareVersion = "v1.9.5",
            macAddress = "68:C6:3A:90:AB:CD"
        ),
        ConnectedDevice(
            id = "DEV_H2WISE_MONITOR",
            name = "H2Wise+ Whole-Home Monitor",
            room = "Utility Basement",
            modelNumber = "K-31422-NA",
            serialNumber = "KHL-2024-11899",
            firmwareVersion = "v2.2.3",
            macAddress = "E4:5F:01:FE:DC:BA"
        )
    )

    private val _devices = MutableStateFlow(defaultDevices)
    override val devices: StateFlow<List<ConnectedDevice>> = _devices.asStateFlow()

    private val _selectedDevice = MutableStateFlow(defaultDevices.first())
    override val selectedDevice: StateFlow<ConnectedDevice> = _selectedDevice.asStateFlow()

    // Default to the slide's signature scenario: Unsupported 5 GHz Wi-Fi Band
    private val _currentNetwork = MutableStateFlow(
        NetworkAttributes(
            ssid = "NETGEAR-Home_5G",
            bssid = "00:14:D1:68:5A:20",
            wifiSignalDbm = -65,
            frequencyBand = WifiBand.BAND_5_GHZ, // 5 GHz failure
            securityType = WifiSecurity.WPA2_PSK,
            isMeshOrExtender = false,
            meshNodeName = null,
            ipAddress = "192.168.1.108",
            gatewayIp = "192.168.1.1",
            dhcpStatus = DhcpStatus.ASSIGNED,
            bleSignalDbm = -64,
            bleState = BleState.CONNECTED,
            cloudPingMs = 45L,
            packetLossPercent = 0
        )
    )
    override val currentNetwork: StateFlow<NetworkAttributes> = _currentNetwork.asStateFlow()

    private val _latestReport = MutableStateFlow<DiagnosticReport?>(null)
    override val latestReport: StateFlow<DiagnosticReport?> = _latestReport.asStateFlow()

    private val _tickets = MutableStateFlow<List<SupportTicket>>(emptyList())
    override val tickets: StateFlow<List<SupportTicket>> = _tickets.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val liveScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        // Run initial diagnostic pass so the UI has immediate rich data
        val initialReport = engine.runDiagnostics(_selectedDevice.value, _currentNetwork.value)
        _latestReport.value = initialReport
        if (enableLiveMonitoring) {
            startLiveMonitoring()
        }
    }

    private fun startLiveMonitoring() {
        liveScope.launch {
            while (isActive) {
                delay(2000)
                if (!_isScanning.value) {
                    val current = _currentNetwork.value
                    val isWifiOff = !current.isWifiEnabled || current.wifiSignalDbm <= -100
                    val isBleOff = !current.isBleEnabled || current.bleState == BleState.BLUETOOTH_OFF || current.bleSignalDbm <= -100

                    if (!isWifiOff && !isBleOff) {
                        val wifiVariation = (-1..1).random()
                        val bleVariation = (-1..1).random()
                        val updatedWifiRssi = (current.wifiSignalDbm + wifiVariation).coerceIn(-90, -40)
                        val updatedBleRssi = (current.bleSignalDbm + bleVariation).coerceIn(-90, -45)

                        val updated = current.copy(
                            wifiSignalDbm = updatedWifiRssi,
                            bleSignalDbm = updatedBleRssi
                        )
                        _currentNetwork.value = updated

                        // Keep check items for signal & BLE in sync with live values
                        _latestReport.value?.let { report ->
                            val updatedChecks = report.checkItems.map { item ->
                                when (item.id) {
                                    "CHECK_SIGNAL" -> if (item.status != CheckStatus.FAIL) item.copy(
                                        summary = "Live Signal ($updatedWifiRssi dBm)",
                                        metricValue = "$updatedWifiRssi dBm (Live)"
                                    ) else item
                                    "CHECK_BLE" -> if (item.status != CheckStatus.FAIL) item.copy(
                                        summary = "BLE Connected ($updatedBleRssi dBm)",
                                        metricValue = "$updatedBleRssi dBm (Live Link)"
                                    ) else item
                                    else -> item
                                }
                            }
                            _latestReport.value = report.copy(
                                network = updated,
                                checkItems = updatedChecks
                            )
                        }
                    }
                }
            }
        }
    }

    override fun selectDevice(device: ConnectedDevice) {
        _selectedDevice.value = device
        _latestReport.value = engine.runDiagnostics(device, _currentNetwork.value)
    }

    override fun loadFailureScenario(scenarioKey: String) {
        val updated = when (scenarioKey) {
            "SCENARIO_5GHZ_BAND" -> NetworkAttributes(
                ssid = "Home_Fast_5G_Only",
                bssid = "20:B0:01:A2:33:90",
                wifiSignalDbm = -64,
                frequencyBand = WifiBand.BAND_5_GHZ,
                securityType = WifiSecurity.WPA2_PSK,
                isMeshOrExtender = false,
                ipAddress = "192.168.1.88",
                gatewayIp = "192.168.1.1",
                dhcpStatus = DhcpStatus.ASSIGNED,
                bleSignalDbm = -66,
                bleState = BleState.CONNECTED,
                cloudPingMs = 39L
            )
            "SCENARIO_WEAK_SIGNAL" -> NetworkAttributes(
                ssid = "Kohler_Main_Floor",
                bssid = "20:B0:01:A2:44:11",
                wifiSignalDbm = -84, // -84 dBm weak signal
                frequencyBand = WifiBand.BAND_2_4_GHZ,
                securityType = WifiSecurity.WPA2_PSK,
                isMeshOrExtender = true,
                meshNodeName = "Basement Repeater (Dist: 65ft)",
                ipAddress = "192.168.1.92",
                gatewayIp = "192.168.1.1",
                dhcpStatus = DhcpStatus.ASSIGNED,
                bleSignalDbm = -88,
                bleState = BleState.OUT_OF_RANGE,
                cloudPingMs = 280L,
                packetLossPercent = 32
            )
            "SCENARIO_WPA3_MISMATCH" -> NetworkAttributes(
                ssid = "Orbi_UltraSecure",
                bssid = "44:D9:E7:88:12:00",
                wifiSignalDbm = -58,
                frequencyBand = WifiBand.BAND_2_4_GHZ,
                securityType = WifiSecurity.WPA3_SAE, // WPA3 only mismatch
                isMeshOrExtender = true,
                meshNodeName = "Orbi Master Node",
                ipAddress = "192.168.50.45",
                gatewayIp = "192.168.50.1",
                dhcpStatus = DhcpStatus.ASSIGNED,
                bleSignalDbm = -61,
                bleState = BleState.CONNECTED,
                cloudPingMs = 31L
            )
            "SCENARIO_DHCP_FAILURE" -> NetworkAttributes(
                ssid = "Kohler_Konnect_24",
                bssid = "00:26:86:14:8B:2E",
                wifiSignalDbm = -62,
                frequencyBand = WifiBand.BAND_2_4_GHZ,
                securityType = WifiSecurity.WPA2_PSK,
                isMeshOrExtender = false,
                ipAddress = "169.254.120.45", // APIPA
                gatewayIp = "0.0.0.0",
                dhcpStatus = DhcpStatus.APIPA_SELF_ASSIGNED,
                bleSignalDbm = -65,
                bleState = BleState.CONNECTED,
                cloudPingMs = -1L,
                packetLossPercent = 100
            )
            "SCENARIO_WIFI_OFF" -> NetworkAttributes(
                ssid = "Wi-Fi Disabled",
                bssid = "00:00:00:00:00:00",
                wifiSignalDbm = -127,
                frequencyBand = WifiBand.BAND_2_4_GHZ,
                securityType = WifiSecurity.WPA2_PSK,
                isMeshOrExtender = false,
                ipAddress = "0.0.0.0",
                gatewayIp = "0.0.0.0",
                dhcpStatus = DhcpStatus.FAILED,
                bleSignalDbm = -65,
                bleState = BleState.CONNECTED,
                cloudPingMs = -1L,
                packetLossPercent = 100,
                isWifiEnabled = false,
                isBleEnabled = true
            )
            "SCENARIO_BLE_OFF" -> NetworkAttributes(
                ssid = "Kohler_Konnect_Home",
                bssid = "00:1A:2B:3C:4D:5E",
                wifiSignalDbm = -58,
                frequencyBand = WifiBand.BAND_2_4_GHZ,
                securityType = WifiSecurity.WPA2_PSK,
                isMeshOrExtender = true,
                meshNodeName = "Master Bath Mesh Node",
                ipAddress = "192.168.1.128",
                gatewayIp = "192.168.1.1",
                dhcpStatus = DhcpStatus.ASSIGNED,
                bleSignalDbm = -127,
                bleState = BleState.BLUETOOTH_OFF,
                cloudPingMs = 42L,
                packetLossPercent = 0,
                isWifiEnabled = true,
                isBleEnabled = false
            )
            "SCENARIO_WIFI_AND_BLE_OFF" -> NetworkAttributes(
                ssid = "Wi-Fi Disabled",
                bssid = "00:00:00:00:00:00",
                wifiSignalDbm = -127,
                frequencyBand = WifiBand.BAND_2_4_GHZ,
                securityType = WifiSecurity.WPA2_PSK,
                isMeshOrExtender = false,
                ipAddress = "0.0.0.0",
                gatewayIp = "0.0.0.0",
                dhcpStatus = DhcpStatus.FAILED,
                bleSignalDbm = -127,
                bleState = BleState.BLUETOOTH_OFF,
                cloudPingMs = -1L,
                packetLossPercent = 100,
                isWifiEnabled = false,
                isBleEnabled = false
            )
            else -> NetworkAttributes(
                ssid = "Kohler_Connected_2.4G",
                bssid = "00:1A:2B:3C:4D:5E",
                wifiSignalDbm = -52, // Optimal
                frequencyBand = WifiBand.BAND_2_4_GHZ,
                securityType = WifiSecurity.WPA2_WPA3_MIXED,
                isMeshOrExtender = true,
                meshNodeName = "Bath Extender (Optimal)",
                ipAddress = "192.168.1.134",
                gatewayIp = "192.168.1.1",
                dhcpStatus = DhcpStatus.ASSIGNED,
                bleSignalDbm = -58,
                bleState = BleState.CONNECTED,
                cloudPingMs = 32L,
                packetLossPercent = 0
            )
        }

        _currentNetwork.value = updated
        _latestReport.value = engine.runDiagnostics(_selectedDevice.value, updated)
    }

    override suspend fun runDiagnostics(): DiagnosticReport {
        _isScanning.value = true
        _latestReport.value = null
        delay(900)
        val sampled = try {
            networkProbe.sampleCurrentNetwork()
        } catch (_: Exception) {
            _currentNetwork.value
        }
        _currentNetwork.value = sampled
        delay(300)
        val freshReport = engine.runDiagnostics(_selectedDevice.value, sampled)
        _latestReport.value = freshReport
        _isScanning.value = false
        return freshReport
    }

    override suspend fun runNetworkDiagnosticCheckForDevice(device: ConnectedDevice): DiagnosticReport {
        _selectedDevice.value = device
        _isScanning.value = true
        _latestReport.value = null
        delay(900)
        val sampled = try {
            networkProbe.sampleCurrentNetwork()
        } catch (_: Exception) {
            _currentNetwork.value
        }
        _currentNetwork.value = sampled
        delay(300)
        val freshReport = engine.runDiagnostics(device, sampled)
        _latestReport.value = freshReport
        _isScanning.value = false
        return freshReport
    }

    override suspend fun submitSupportTicket(
        category: String,
        customerNote: String
    ): SupportTicket {
        val dev = _selectedDevice.value
        val net = _currentNetwork.value
        val report = _latestReport.value

        val primaryIssue = report?.issues?.firstOrNull()
        val randomSuffix = (1000..9999).random()
        val timeStampStr = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val ticketId = "KOHLER-ESC-$timeStampStr-$randomSuffix"

        val ticket = SupportTicket(
            ticketId = ticketId,
            timestamp = System.currentTimeMillis(),
            deviceName = dev.name,
            deviceModel = dev.modelNumber,
            deviceFirmware = dev.firmwareVersion,
            deviceMac = dev.macAddress,
            issueCategory = category,
            customerNote = customerNote.ifBlank { "User requested escalation after diagnostic check." },
            detectedErrorCode = primaryIssue?.errorCode ?: "NO_HARD_FAULT_DETECTED",
            failureReason = primaryIssue?.specificFailureReason ?: "Optimal connectivity reported.",
            networkSnapshot = net,
            status = "Escalated to Engineering with Full Diagnostic Context"
        )

        _tickets.value = listOf(ticket) + _tickets.value
        return ticket
    }
}
