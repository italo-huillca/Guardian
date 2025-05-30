package com.midam.guardian

import android.app.Application
import android.content.Intent
import com.google.firebase.FirebaseApp
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.midam.guardian.presentation.navigation.*
import com.midam.guardian.service.BackgroundMqttService
import com.midam.guardian.ui.theme.GuardianTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        
        // Iniciar el servicio MQTT en segundo plano
        startService(Intent(this, BackgroundMqttService::class.java))
        
        setContent {
            GuardianTheme {
                AppNavigation()
            }
        }
    }
}
