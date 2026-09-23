package com.example.common.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.common.domain.engine.DiagnosticReport
import com.example.common.domain.model.ConnectedDevice
import com.example.common.domain.model.NetworkAttributes
import com.example.common.domain.model.SupportTicket
import com.example.common.domain.repository.DiagnosticsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SmartDiagnosticsViewModel(
    private val repository: DiagnosticsRepository
) : ViewModel() {

    val devices: StateFlow<List<ConnectedDevice>> = repository.devices
    val selectedDevice: StateFlow<ConnectedDevice> = repository.selectedDevice
    val currentNetwork: StateFlow<NetworkAttributes> = repository.currentNetwork
    val latestReport: StateFlow<DiagnosticReport?> = repository.latestReport
    val tickets: StateFlow<List<SupportTicket>> = repository.tickets
    val isScanning: StateFlow<Boolean> = repository.isScanning

    fun onSelectDevice(device: ConnectedDevice) {
        repository.selectDevice(device)
    }

    fun onSelectScenario(scenarioKey: String) {
        repository.loadFailureScenario(scenarioKey)
    }

    fun onRunDiagnostics() {
        viewModelScope.launch {
            repository.runDiagnostics()
        }
    }

    fun simulateNetworkDiagnosticCheck(device: ConnectedDevice) {
        viewModelScope.launch {
            repository.runNetworkDiagnosticCheckForDevice(device)
        }
    }

    fun onApplyQuickFixAndRetest() {
        viewModelScope.launch {
            // Switch to optimal resolved scenario and re-run diagnostics
            repository.loadFailureScenario("SCENARIO_OPTIMAL")
            repository.runDiagnostics()
        }
    }

    fun onSubmitTicket(
        category: String,
        note: String,
        onSuccess: (SupportTicket) -> Unit
    ) {
        viewModelScope.launch {
            val ticket = repository.submitSupportTicket(category, note)
            onSuccess(ticket)
        }
    }
}
