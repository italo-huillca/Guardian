package com.midam.guardian.presentation.screen.history

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.midam.guardian.data.model.HistoryLocationData
import com.midam.guardian.data.model.Coordenadas
import com.midam.guardian.data.model.UbicacionInfo
import com.midam.guardian.data.model.DetallesDireccion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "HistoryViewModel"
    
    private val _historyData = MutableStateFlow<List<HistoryLocationData>>(emptyList())
    val historyData: StateFlow<List<HistoryLocationData>> = _historyData.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _selectedDate = MutableStateFlow("")
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()
    
    private val database = FirebaseDatabase.getInstance("https://mochila-guardian.firebaseio.com")
    private val historialRef = database.getReference("historial/kid1")
    
    init {
        loadRecentHistory()
    }
    
    fun loadRecentHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            
            // Cargar últimas 10 ubicaciones
            historialRef.orderByKey()
                .limitToLast(10)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        viewModelScope.launch {
                            try {
                                val historyList = mutableListOf<HistoryLocationData>()
                                
                                for (childSnapshot in snapshot.children) {
                                    val historyItem = parseHistoryItem(childSnapshot)
                                    if (historyItem != null) {
                                        historyList.add(historyItem)
                                    }
                                }
                                
                                // Ordenar por timestamp descendente (más reciente primero)
                                historyList.sortByDescending { it.timestamp }
                                
                                _historyData.value = historyList
                                Log.d(TAG, "Historial cargado: ${historyList.size} elementos")
                                
                            } catch (e: Exception) {
                                Log.e(TAG, "Error al procesar historial: ${e.message}")
                                _historyData.value = emptyList()
                            }
                            _isLoading.value = false
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Error de base de datos: ${error.message}")
                        viewModelScope.launch {
                            _historyData.value = emptyList()
                            _isLoading.value = false
                        }
                    }
                })
        }
    }
    
    fun loadHistoryByDate(date: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _selectedDate.value = date
            
            // Consultar historial por fecha específica
            historialRef.orderByKey()
                .startAt(date)
                .endAt("${date}z")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        viewModelScope.launch {
                            try {
                                val historyList = mutableListOf<HistoryLocationData>()
                                
                                for (childSnapshot in snapshot.children) {
                                    val historyItem = parseHistoryItem(childSnapshot)
                                    if (historyItem != null) {
                                        historyList.add(historyItem)
                                    }
                                }
                                
                                // Ordenar por timestamp descendente
                                historyList.sortByDescending { it.timestamp }
                                
                                _historyData.value = historyList
                                Log.d(TAG, "Historial por fecha $date cargado: ${historyList.size} elementos")
                                
                            } catch (e: Exception) {
                                Log.e(TAG, "Error al procesar historial por fecha: ${e.message}")
                                _historyData.value = emptyList()
                            }
                            _isLoading.value = false
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Error de base de datos: ${error.message}")
                        viewModelScope.launch {
                            _historyData.value = emptyList()
                            _isLoading.value = false
                        }
                    }
                })
        }
    }
    
    private fun parseHistoryItem(snapshot: DataSnapshot): HistoryLocationData? {
        return try {
            // Obtener datos de coordenadas
            val coordenadas = snapshot.child("coordenadas").let { coordSnapshot ->
                Coordenadas(
                    lat = coordSnapshot.child("lat").getValue(Double::class.java) ?: 0.0,
                    lon = coordSnapshot.child("lon").getValue(Double::class.java) ?: 0.0
                )
            }
            
            // Obtener información de ubicación
            val ubicacion = snapshot.child("ubicacion").let { ubicSnapshot ->
                val detalles = ubicSnapshot.child("detalles").let { detallesSnapshot ->
                    DetallesDireccion(
                        calle = detallesSnapshot.child("calle").getValue(String::class.java) ?: "",
                        numero = detallesSnapshot.child("numero").getValue(String::class.java) ?: "",
                        barrio = detallesSnapshot.child("barrio").getValue(String::class.java) ?: "",
                        ciudad = detallesSnapshot.child("ciudad").getValue(String::class.java) ?: "",
                        estado = detallesSnapshot.child("estado").getValue(String::class.java) ?: "",
                        pais = detallesSnapshot.child("pais").getValue(String::class.java) ?: ""
                    )
                }
                
                UbicacionInfo(
                    direccion_completa = ubicSnapshot.child("direccion_completa").getValue(String::class.java) ?: "Ubicación no disponible",
                    detalles = detalles
                )
            }
            
            // Crear el objeto HistoryLocationData
            HistoryLocationData(
                device_id = snapshot.child("device_id").getValue(String::class.java) ?: "kid1",
                timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L,
                fecha_legible = snapshot.child("fecha_legible").getValue(String::class.java) ?: formatTimestamp(snapshot.child("timestamp").getValue(Long::class.java) ?: 0L),
                coordenadas = coordenadas,
                ubicacion = ubicacion
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error al parsear item del historial: ${e.message}")
            null
        }
    }
    
    private fun formatTimestamp(timestamp: Long): String {
        return if (timestamp > 0) {
            val sdf = SimpleDateFormat("dd/MM/yyyy, HH:mm:ss", Locale.getDefault())
            sdf.format(Date(timestamp))
        } else {
            "Fecha no disponible"
        }
    }
    
    fun clearSelectedDate() {
        _selectedDate.value = ""
        loadRecentHistory()
    }
} 