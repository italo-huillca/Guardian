package com.midam.guardian.presentation.screen.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.midam.guardian.model.EmergencyMessage
import com.midam.guardian.service.MqttService
import com.midam.guardian.service.NotificationService
import kotlinx.coroutines.launch

data class EmergencyAlert(
    val emergencyMessage: EmergencyMessage,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var alerts by remember { mutableStateOf(listOf<EmergencyAlert>()) }
    
    val mqttService = remember { MqttService(context) }
    val notificationService = remember { NotificationService(context) }

    LaunchedEffect(Unit) {
        mqttService.connect(
            onConnected = {
                mqttService.subscribe("gps/emergencia") { message ->
                    val alert = EmergencyAlert(message)
                    alerts = alerts + alert
                    notificationService.showEmergencyNotification(
                        "Tipo: ${message.alerta}\nMensaje: ${message.mensaje}"
                    )
                }
            },
            onError = { /* Manejar error */ }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            mqttService.disconnect()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notificaciones") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        if (alerts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    text = "No hay alertas de emergencia",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
                items(alerts.reversed()) { alert ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "¡Alerta de Emergencia!",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tipo: ${alert.emergencyMessage.alerta}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
            Text(
                                text = "Mensaje: ${alert.emergencyMessage.mensaje}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
            )
                            Spacer(modifier = Modifier.height(8.dp))
            Text(
                                text = "Recibido: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(alert.timestamp)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
            )
                        }
                    }
                }
            }
        }
    }
} 