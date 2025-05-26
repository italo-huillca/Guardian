package com.midam.guardian.service

import android.content.Context
import android.util.Log
import com.midam.guardian.model.EmergencyMessage
import kotlinx.serialization.json.Json
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MqttService(private val context: Context) {
    private val TAG = "MqttService"
    private val serverUri = "tcp://161.132.45.106:1883"
    private val clientId = "GuardianAndroid_${System.currentTimeMillis()}"
    private val mqttClient = MqttClient(serverUri, clientId, MemoryPersistence())
    private val json = Json { ignoreUnknownKeys = true }
    
    private var isConnected = false

    fun connect(onConnected: () -> Unit = {}, onError: (String) -> Unit = {}) {
        try {
            mqttClient.connect()
            isConnected = true
            Log.d(TAG, "Conexión MQTT exitosa")
            onConnected()
        } catch (e: MqttException) {
            isConnected = false
            Log.e(TAG, "Error al conectar MQTT: ${e.message}")
            onError(e.message ?: "Error desconocido")
        }
    }

    fun subscribe(topic: String, onMessage: (EmergencyMessage) -> Unit) {
        if (!isConnected) {
            Log.e(TAG, "No hay conexión MQTT activa")
            return
        }

        try {
            mqttClient.subscribe(topic) { _, message ->
                try {
                    val payload = String(message.payload)
                    Log.d(TAG, "Mensaje recibido en $topic: $payload")
                    val emergencyMessage = json.decodeFromString<EmergencyMessage>(payload)
                    onMessage(emergencyMessage)
                } catch (e: Exception) {
                    Log.e(TAG, "Error al procesar mensaje: ${e.message}")
                }
            }
        } catch (e: MqttException) {
            Log.e(TAG, "Error al suscribirse al topic $topic: ${e.message}")
        }
    }

    fun disconnect() {
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