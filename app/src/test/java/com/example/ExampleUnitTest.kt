package com.example

import com.example.common.domain.engine.SmartDiagnosticsEngine
import com.example.common.domain.model.BleState
import com.example.common.domain.model.ConnectedDevice
import com.example.common.domain.model.DhcpStatus
import com.example.common.domain.model.DiagnosticSeverity
import com.example.common.domain.model.NetworkAttributes
import com.example.common.domain.model.WifiBand
import com.example.common.domain.model.WifiSecurity
import com.example.common.data.InMemoryDiagnosticsRepository
import com.example.common.data.probe.NetworkProbe
import com.example.common.ui.SmartDiagnosticsViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun `engine flags optimal network when 2_4 GHz and strong signal`() {
    val engine = SmartDiagnosticsEngine()
    val device = ConnectedDevice(
      id = "DEV_TEST",
      name = "DTV+ Shower",
      room = "Master Bath",
      modelNumber = "K-99695-NA",
      serialNumber = "SN-TEST",
      firmwareVersion = "v3.14.2",
      macAddress = "3C:71:BF:88:21:4A"
    )
    val network = NetworkAttributes(
      ssid = "Home_2.4G",
      bssid = "00:11:22:33:44:55",
      wifiSignalDbm = -52,
      frequencyBand = WifiBand.BAND_2_4_GHZ,
      securityType = WifiSecurity.WPA2_PSK,
      isMeshOrExtender = false,
      ipAddress = "192.168.1.100",
      gatewayIp = "192.168.1.1",
      dhcpStatus = DhcpStatus.ASSIGNED,
      bleSignalDbm = -50,
      bleState = BleState.CONNECTED
    )

    val report = engine.runDiagnostics(device, network)
    assertEquals(DiagnosticSeverity.OPTIMAL, report.overallSeverity)
    assertTrue(report.issues.isEmpty())
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `viewModel simulateNetworkDiagnosticCheck runs check on selected device`() = runTest {
    val testDispatcher = StandardTestDispatcher(testScheduler)
    kotlinx.coroutines.Dispatchers.setMain(testDispatcher)
    try {
      val mockProbe = object : NetworkProbe {
        override suspend fun sampleCurrentNetwork(): NetworkAttributes =
          NetworkAttributes(
            ssid = "Test_WiFi",
            bssid = "00:11:22:33:44:55",
            wifiSignalDbm = -55,
            frequencyBand = WifiBand.BAND_2_4_GHZ,
            securityType = WifiSecurity.WPA2_PSK,
            isMeshOrExtender = false,
            ipAddress = "192.168.1.100",
            gatewayIp = "192.168.1.1",
            dhcpStatus = DhcpStatus.ASSIGNED,
            bleSignalDbm = -50,
            bleState = BleState.CONNECTED
          )
      }
      val repo = InMemoryDiagnosticsRepository(networkProbe = mockProbe, enableLiveMonitoring = false)
      val viewModel = SmartDiagnosticsViewModel(repo)

      val targetDevice = repo.devices.value.last() // e.g. H2Wise monitor
      viewModel.simulateNetworkDiagnosticCheck(targetDevice)
      advanceUntilIdle()

      assertEquals(targetDevice.id, viewModel.selectedDevice.value.id)
      assertNotNull(viewModel.latestReport.value)
      assertEquals(targetDevice.id, viewModel.latestReport.value?.device?.id)
    } finally {
      kotlinx.coroutines.Dispatchers.resetMain()
    }
  }

  @Test
  fun `engine flags error when wifi is disabled`() {
    val engine = SmartDiagnosticsEngine()
    val device = ConnectedDevice(
      id = "DEV_TEST",
      name = "DTV+ Shower",
      room = "Master Bath",
      modelNumber = "K-99695-NA",
      serialNumber = "SN-TEST",
      firmwareVersion = "v3.14.2",
      macAddress = "3C:71:BF:88:21:4A"
    )
    val network = NetworkAttributes(
      ssid = "Wi-Fi Disabled",
      bssid = "00:00:00:00:00:00",
      wifiSignalDbm = -127,
      frequencyBand = WifiBand.BAND_2_4_GHZ,
      securityType = WifiSecurity.WPA2_PSK,
      isMeshOrExtender = false,
      ipAddress = "0.0.0.0",
      gatewayIp = "0.0.0.0",
      dhcpStatus = DhcpStatus.FAILED,
      bleSignalDbm = -60,
      bleState = BleState.CONNECTED,
      isWifiEnabled = false,
      isBleEnabled = true
    )

    val report = engine.runDiagnostics(device, network)
    assertEquals(DiagnosticSeverity.CRITICAL, report.overallSeverity)
    assertTrue(report.issues.any { it.errorCode == "ERR_WIFI_DISABLED" })
  }

  @Test
  fun `engine flags error when bluetooth is disabled`() {
    val engine = SmartDiagnosticsEngine()
    val device = ConnectedDevice(
      id = "DEV_TEST",
      name = "DTV+ Shower",
      room = "Master Bath",
      modelNumber = "K-99695-NA",
      serialNumber = "SN-TEST",
      firmwareVersion = "v3.14.2",
      macAddress = "3C:71:BF:88:21:4A"
    )
    val network = NetworkAttributes(
      ssid = "Home_2.4G",
      bssid = "00:11:22:33:44:55",
      wifiSignalDbm = -52,
      frequencyBand = WifiBand.BAND_2_4_GHZ,
      securityType = WifiSecurity.WPA2_PSK,
      isMeshOrExtender = false,
      ipAddress = "192.168.1.100",
      gatewayIp = "192.168.1.1",
      dhcpStatus = DhcpStatus.ASSIGNED,
      bleSignalDbm = -127,
      bleState = BleState.BLUETOOTH_OFF,
      isWifiEnabled = true,
      isBleEnabled = false
    )

    val report = engine.runDiagnostics(device, network)
    assertEquals(DiagnosticSeverity.CRITICAL, report.overallSeverity)
    assertTrue(report.issues.any { it.errorCode == "ERR_BLUETOOTH_DISABLED" })
  }
}

