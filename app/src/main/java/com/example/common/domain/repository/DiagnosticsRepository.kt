package com.example.common.domain.repository

import com.example.common.domain.engine.DiagnosticReport
import com.example.common.domain.model.ConnectedDevice
import com.example.common.domain.model.NetworkAttributes
import com.example.common.domain.model.SupportTicket
import kotlinx.coroutines.flow.StateFlow

interface DiagnosticsRepository {
    val devices: StateFlow<List<ConnectedDevice>>
    val selectedDevice: StateFlow<ConnectedDevice>
    val currentNetwork: StateFlow<NetworkAttributes>
    val latestReport: StateFlow<DiagnosticReport?>
    val tickets: StateFlow<List<SupportTicket>>
    val isScanning: StateFlow<Boolean>

    fun selectDevice(device: ConnectedDevice)
    fun loadFailureScenario(scenarioKey: String)
    suspend fun runDiagnostics(): DiagnosticReport
    suspend fun runNetworkDiagnosticCheckForDevice(device: ConnectedDevice): DiagnosticReport
    suspend fun submitSupportTicket(
        category: String,
        customerNote: String
    ): SupportTicket
}
