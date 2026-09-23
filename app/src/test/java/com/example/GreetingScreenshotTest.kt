package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.common.data.InMemoryDiagnosticsRepository
import com.example.common.data.probe.NetworkProbe
import com.example.common.domain.model.CheckStatus
import com.example.common.domain.model.DiagnosticCheckItem
import com.example.common.domain.model.NetworkAttributes
import com.example.common.domain.model.WifiBand
import com.example.common.domain.model.WifiSecurity
import com.example.common.domain.model.DhcpStatus
import com.example.common.domain.model.BleState
import com.example.common.ui.SmartDiagnosticsApp
import com.example.common.ui.SmartDiagnosticsViewModel
import com.example.common.ui.components.DiagnosticCheckRow
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun full_app_screenshot() {
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

    composeTestRule.setContent {
      MyApplicationTheme {
        SmartDiagnosticsApp(viewModel = viewModel)
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/app_dashboard.png")
  }

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        DiagnosticCheckRow(
          item = DiagnosticCheckItem(
            id = "test_check",
            title = "Wi-Fi Frequency Band",
            category = "Band Compatibility",
            status = CheckStatus.PASS,
            summary = "Compatible 2.4 GHz IoT band",
            detail = "Hardware requires 2.4 GHz IoT band",
            metricValue = "2.4 GHz"
          )
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}

