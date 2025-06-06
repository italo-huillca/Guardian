package com.midam.guardian.presentation.screen.map

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DeviceStatus(
    val isConnected: Boolean = false,
    val lastUpdateTime: Long = 0,
    val batteryLevel: Int = 0,
    val signalStrength: String = "Sin señal"
)

data class LocationData(
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val timestamp: Long = 0
)

class DeviceStatusViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "DeviceStatusViewModel"
    private val CONNECTION_TIMEOUT = 30000L // 30 segundos
    
    private val _deviceStatus = MutableStateFlow(DeviceStatus())
    val deviceStatus: StateFlow<DeviceStatus> = _deviceStatus.asStateFlow()
    
    private val _locationData = MutableStateFlow(LocationData())
    val locationData: StateFlow<LocationData> = _locationData.asStateFlow()
    
    private val database = FirebaseDatabase.getInstance("https://mochila-guardian.firebaseio.com")
    private val ubicacionesRef = database.getReference("ubicaciones/kid1")
    
    init {
        startMonitoring()
    }
    
    private fun startMonitoring() {
        ubicacionesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                viewModelScope.launch {
                    try {
                        val lat = snapshot.child("lat").getValue(Double::class.java) ?: 0.0
                        val lon = snapshot.child("lon").getValue(Double::class.java) ?: 0.0
                        val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                        
                        Log.d(TAG, "Datos recibidos: lat=$lat, lon=$lon, timestamp=$timestamp")
                        
                        _locationData.value = LocationData(lat, lon, timestamp)
                        
                        // Verificar si el dispositivo está conectado basado en el timestamp
                        val currentTime = System.currentTimeMillis()
                        val timeDifference = currentTime - timestamp
                        val isConnected = timeDifference <= CONNECTION_TIMEOUT
                        
                        Log.d(TAG, "Diferencia de tiempo: ${timeDifference}ms, Conectado: $isConnected")
                        
                        _deviceStatus.value = _deviceStatus.value.copy(
                            isConnected = isConnected,
                            lastUpdateTime = timestamp,
                            signalStrength = if (isConnected) "Buena señal" else "Sin señal"
                        )
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al procesar datos: ${e.message}")
                        _deviceStatus.value = _deviceStatus.value.copy(
                            isConnected = false,
                            signalStrength = "Error de conexión"
                        )
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error de base de datos: ${error.message}")
                viewModelScope.launch {
                    _deviceStatus.value = _deviceStatus.value.copy(
                        isConnected = false,
                        signalStrength = "Error de base de datos"
                    )
                }
            }
        })
        
        // Verificación periódica del estado de conexión
        startPeriodicCheck()
    }
    
    private fun startPeriodicCheck() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(5000) // Verificar cada 5 segundos
                val currentTime = System.currentTimeMillis()
                val lastUpdate = _deviceStatus.value.lastUpdateTime
                val timeDifference = currentTime - lastUpdate
                
                if (timeDifference > CONNECTION_TIMEOUT && _deviceStatus.value.isConnected) {
                    Log.d(TAG, "Dispositivo desconectado por timeout")
                    _deviceStatus.value = _deviceStatus.value.copy(
                        isConnected = false,
                        signalStrength = "Dispositivo desconectado"
                    )
                }
            }
        }
    }
    
    fun refreshStatus() {
        startMonitoring()
    }
} 