package com.example.common.data.probe

import com.example.common.domain.model.NetworkAttributes

/**
 * KMP Network Probe interface.
 * In a Kotlin Multiplatform project, expect/actual or platform implementations
 * query device hardware APIs on Android (WifiManager/ConnectivityManager) and iOS (Network framework).
 */
interface NetworkProbe {
    suspend fun sampleCurrentNetwork(): NetworkAttributes
}

/**
 * KMP Bluetooth Probe interface.
 * Measures BLE RSSI and connection state to the smart home IoT device.
 */
interface BluetoothProbe {
    suspend fun sampleBleRssi(macAddress: String): Int
}
