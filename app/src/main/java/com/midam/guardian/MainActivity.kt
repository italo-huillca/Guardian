package com.midam.guardian

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.google.firebase.FirebaseApp
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.midam.guardian.presentation.navigation.*
import com.midam.guardian.presentation.screen.notifications.NotificationsViewModel
import com.midam.guardian.service.BackgroundMqttService
import com.midam.guardian.ui.theme.GuardianTheme
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private var mqttService: BackgroundMqttService? = null
    private var bound = false
    private var notificationsViewModel: NotificationsViewModel? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as BackgroundMqttService.LocalBinder
            mqttService = binder.getService()
            bound = true
            
            // Configurar el ViewModel si ya está disponible
            notificationsViewModel?.let { viewModel ->
                mqttService?.setViewModel(viewModel)
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            bound = false
        }
    }

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            // Pedes manejar la respuesta aquí si quieres
        }

    private fun requestAllPermissionsIfNeeded() {
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        requestAllPermissionsIfNeeded()
        
        // Iniciar y vincular el servicio MQTT
        Intent(this, BackgroundMqttService::class.java).also { intent ->
            startService(intent)
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
        
        setContent {
            GuardianTheme {
                val viewModel: NotificationsViewModel = viewModel()
                
                // Almacenar el ViewModel para uso en el ServiceConnection
                LaunchedEffect(viewModel) {
                    notificationsViewModel = viewModel
                    mqttService?.setViewModel(viewModel)
                }
                
                AppNavigation(notificationsViewModel = viewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (bound) {
            unbindService(connection)
            bound = false
        }
    }
}
