package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.common.data.InMemoryDiagnosticsRepository
import com.example.common.ui.SmartDiagnosticsApp
import com.example.common.ui.SmartDiagnosticsViewModel
import com.example.platform.AndroidNetworkProbe
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: SmartDiagnosticsViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val networkProbe = AndroidNetworkProbe(applicationContext)
                val repository = InMemoryDiagnosticsRepository(networkProbe)
                return SmartDiagnosticsViewModel(repository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                SmartDiagnosticsApp(viewModel = viewModel)
            }
        }
    }
}

