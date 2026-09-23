package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.common.domain.engine.SmartDiagnosticsEngine
import com.example.common.domain.model.BleState
import com.example.common.domain.model.ConnectedDevice
import com.example.common.domain.model.DhcpStatus
import com.example.common.domain.model.DiagnosticSeverity
import com.example.common.domain.model.NetworkAttributes
import com.example.common.domain.model.WifiBand
import com.example.common.domain.model.WifiSecurity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `activity launches successfully`() {
    val controller = Robolectric.buildActivity(MainActivity::class.java)
    controller.setup()
    assertNotNull(controller.get())
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Smart Diagnostics", appName)
  }

  @Test
  fun `smart diagnostics engine detects 5ghz band mismatch`() {
    val engine = SmartDiagnosticsEngine()
    val device = ConnectedDevice(
      id = "DEV_1",
      name = "DTV+ Digital Shower System",
      room = "Master Bath",
      modelNumber = "K-99695-NA",
      serialNumber = "SN-12345",
      firmwareVersion = "v3.14.2",
      macAddress = "3C:71:BF:88:21:4A"
    )
    val network = NetworkAttributes(
      ssid = "Home_5G_Only",
      bssid = "00:11:22:33:44:55",
      wifiSignalDbm = -65,
      frequencyBand = WifiBand.BAND_5_GHZ,
      securityType = WifiSecurity.WPA2_PSK,
      isMeshOrExtender = false,
      ipAddress = "192.168.1.100",
      gatewayIp = "192.168.1.1",
      dhcpStatus = DhcpStatus.ASSIGNED,
      bleSignalDbm = -65,
      bleState = BleState.CONNECTED
    )

    val report = engine.runDiagnostics(device, network)
    assertEquals(DiagnosticSeverity.CRITICAL, report.overallSeverity)
    assertTrue(report.issues.any { it.errorCode == "ERR_WIFI_5GHZ_BAND_MISMATCH" })
    val issue = report.issues.first { it.errorCode == "ERR_WIFI_5GHZ_BAND_MISMATCH" }
    assertEquals("Your router is using 5 GHz only. This product requires a 2.4 GHz network.", issue.customerFriendlyExplanation)
  }
}

