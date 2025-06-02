package com.midam.guardian.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.midam.guardian.MainActivity
import com.midam.guardian.R
import com.midam.guardian.model.EmergencyMessage
import com.midam.guardian.presentation.screen.notifications.NotificationsViewModel
import kotlinx.serialization.json.Json
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class BackgroundMqttService : Service() {
    private val TAG = "BackgroundMqttService"
    private val serverUri = "tcp://161.132.45.106:1883"
    private val clientId = "GuardianAndroid_${System.currentTimeMillis()}"
    private val mqttClient = MqttClient(serverUri, clientId, MemoryPersistence())
    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var notificationService: NotificationService
    private var viewModel: NotificationsViewModel? = null
    
    private var isConnected = false

    // Binder para vincular el servicio
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): BackgroundMqttService = this@BackgroundMqttService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        notificationService = NotificationService(this)
        connectMqtt()
    }

    fun setViewModel(viewModel: NotificationsViewModel) {
        this.viewModel = viewModel
        notificationService.setViewModel(viewModel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun connectMqtt() {
        try {
            mqttClient.connect()
            isConnected = true
            Log.d(TAG, "Conexión MQTT exitosa")
            subscribeToTopic()
        } catch (e: MqttException) {
            isConnected = false
            Log.e(TAG, "Error al conectar MQTT: ${e.message}")
            // Intentar reconectar después de un error
            android.os.Handler(mainLooper).postDelayed({ connectMqtt() }, 5000)
        }
    }

    private fun subscribeToTopic() {
        if (!isConnected) {
            Log.e(TAG, "No hay conexión MQTT activa")
            return
        }

        try {
            mqttClient.subscribe("gps/emergencia") { _, message ->
                try {
                    val payload = String(message.payload)
                    Log.d(TAG, "Mensaje recibido en gps/emergencia: $payload")
                    val emergencyMessage = json.decodeFromString<EmergencyMessage>(payload)
                    notificationService.showEmergencyNotification(
                        "Tipo: ${emergencyMessage.alerta}\nMensaje: ${emergencyMessage.mensaje}"
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error al procesar mensaje: ${e.message}")
                }
            }
        } catch (e: MqttException) {
            Log.e(TAG, "Error al suscribirse al topic: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (isConnected) {
                mqttClient.disconnect()
                isConnected = false
                Log.d(TAG, "Desconexión MQTT exitosa")
            }
        } catch (e: MqttException) {
            Log.e(TAG, "Error al desconectar MQTT: ${e.message}")
        }
    }
} 