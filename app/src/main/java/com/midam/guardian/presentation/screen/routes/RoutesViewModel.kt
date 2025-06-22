package com.midam.guardian.presentation.screen.routes

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.midam.guardian.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

class RoutesViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "RoutesViewModel"
    
    private val _routes = MutableStateFlow<List<SafeRoute>>(emptyList())
    val routes: StateFlow<List<SafeRoute>> = _routes.asStateFlow()
    
    private val _safeZones = MutableStateFlow<List<SafeZone>>(emptyList())
    val safeZones: StateFlow<List<SafeZone>> = _safeZones.asStateFlow()
    
    private val _alerts = MutableStateFlow<List<GeofenceAlert>>(emptyList())
    val alerts: StateFlow<List<GeofenceAlert>> = _alerts.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _selectedRoute = MutableStateFlow<SafeRoute?>(null)
    val selectedRoute: StateFlow<SafeRoute?> = _selectedRoute.asStateFlow()
    
    private val _lastKnownLocation = MutableStateFlow<LatLng?>(null)
    val lastKnownLocation: StateFlow<LatLng?> = _lastKnownLocation.asStateFlow()
    
    private val database = FirebaseDatabase.getInstance("https://mochila-guardian.firebaseio.com")
    private val routesRef = database.getReference("geofencing/routes")
    private val zonesRef = database.getReference("geofencing/zones")
    private val alertsRef = database.getReference("geofencing/alerts")
    private val locationRef = database.getReference("ubicaciones/kid1")
    
    init {
        loadRoutes()
        loadSafeZones()
        loadAlerts()
        loadLastLocation()
    }
    
    private fun loadLastLocation() {
        locationRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val lat = snapshot.child("lat").getValue(Double::class.java)
                    val lon = snapshot.child("lon").getValue(Double::class.java)
                    if (lat != null && lon != null) {
                        _lastKnownLocation.value = LatLng(lat, lon)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error al cargar última ubicación: ${e.message}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error al cargar ubicación: ${error.message}")
            }
        })
    }
    
    fun loadRoutes() {
        viewModelScope.launch {
            _isLoading.value = true
            
            routesRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    viewModelScope.launch {
                        try {
                            val routesList = mutableListOf<SafeRoute>()
                            
                            for (childSnapshot in snapshot.children) {
                                val route = parseRoute(childSnapshot)
                                if (route != null) {
                                    routesList.add(route)
                                }
                            }
                            
                            _routes.value = routesList.sortedByDescending { it.createdAt }
                            Log.d(TAG, "Rutas cargadas: ${routesList.size}")
                            
                        } catch (e: Exception) {
                            Log.e(TAG, "Error al cargar rutas: ${e.message}")
                        }
                        _isLoading.value = false
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error de base de datos: ${error.message}")
                    viewModelScope.launch { _isLoading.value = false }
                }
            })
        }
    }
    
    fun loadSafeZones() {
        viewModelScope.launch {
            zonesRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    viewModelScope.launch {
                        try {
                            val zonesList = mutableListOf<SafeZone>()
                            
                            for (childSnapshot in snapshot.children) {
                                val zone = parseSafeZone(childSnapshot)
                                if (zone != null) {
                                    zonesList.add(zone)
                                }
                            }
                            
                            _safeZones.value = zonesList.sortedByDescending { it.createdAt }
                            Log.d(TAG, "Zonas seguras cargadas: ${zonesList.size}")
                            
                        } catch (e: Exception) {
                            Log.e(TAG, "Error al cargar zonas: ${e.message}")
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error de base de datos: ${error.message}")
                }
            })
        }
    }
    
    fun loadAlerts() {
        viewModelScope.launch {
            alertsRef.orderByChild("timestamp")
                .limitToLast(20)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        viewModelScope.launch {
                            try {
                                val alertsList = mutableListOf<GeofenceAlert>()
                                
                                for (childSnapshot in snapshot.children) {
                                    val alert = parseAlert(childSnapshot)
                                    if (alert != null) {
                                        alertsList.add(alert)
                                    }
                                }
                                
                                _alerts.value = alertsList.sortedByDescending { it.timestamp }
                                Log.d(TAG, "Alertas cargadas: ${alertsList.size}")
                                
                            } catch (e: Exception) {
                                Log.e(TAG, "Error al cargar alertas: ${e.message}")
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Error de base de datos: ${error.message}")
                    }
                })
        }
    }
    
    fun createRoute(name: String, description: String, points: List<LatLng>, radius: Double, color: String) {
        viewModelScope.launch {
            try {
                val routeId = UUID.randomUUID().toString()
                val routePoints = points.mapIndexed { index, latLng ->
                    RoutePoint(
                        lat = latLng.latitude,
                        lon = latLng.longitude,
                        order = index,
                        name = if (index == 0) "Inicio" else if (index == points.size - 1) "Destino" else "Punto ${index + 1}"
                    )
                }
                
                val route = SafeRoute(
                    id = routeId,
                    name = name,
                    description = description,
                    points = routePoints,
                    radius = radius,
                    color = color
                )
                
                routesRef.child(routeId).setValue(route)
                Log.d(TAG, "Ruta creada: $name")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error al crear ruta: ${e.message}")
            }
        }
    }
    
    fun updateRoute(route: SafeRoute) {
        viewModelScope.launch {
            try {
                routesRef.child(route.id).setValue(route)
                Log.d(TAG, "Ruta actualizada: ${route.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Error al actualizar ruta: ${e.message}")
            }
        }
    }
    
    fun deleteRoute(routeId: String) {
        viewModelScope.launch {
            try {
                routesRef.child(routeId).removeValue()
                Log.d(TAG, "Ruta eliminada: $routeId")
            } catch (e: Exception) {
                Log.e(TAG, "Error al eliminar ruta: ${e.message}")
            }
        }
    }
    
    fun toggleRouteActive(routeId: String, isActive: Boolean) {
        viewModelScope.launch {
            try {
                routesRef.child(routeId).child("isActive").setValue(isActive)
                Log.d(TAG, "Estado de ruta cambiado: $routeId -> $isActive")
            } catch (e: Exception) {
                Log.e(TAG, "Error al cambiar estado: ${e.message}")
            }
        }
    }
    
    fun createSafeZone(name: String, description: String, center: LatLng, radius: Double, zoneType: SafeZoneType, color: String) {
        viewModelScope.launch {
            try {
                val zoneId = UUID.randomUUID().toString()
                val zone = SafeZone(
                    id = zoneId,
                    name = name,
                    description = description,
                    center = Coordenadas(center.latitude, center.longitude),
                    radius = radius,
                    zoneType = zoneType,
                    color = color
                )
                
                zonesRef.child(zoneId).setValue(zone)
                Log.d(TAG, "Zona segura creada: $name")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error al crear zona: ${e.message}")
            }
        }
    }
    
    fun markAlertAsRead(alertId: String) {
        viewModelScope.launch {
            try {
                alertsRef.child(alertId).child("isRead").setValue(true)
            } catch (e: Exception) {
                Log.e(TAG, "Error al marcar alerta: ${e.message}")
            }
        }
    }
    
    fun selectRoute(route: SafeRoute?) {
        _selectedRoute.value = route
    }
    
    private fun parseRoute(snapshot: DataSnapshot): SafeRoute? {
        return try {
            val pointsList = mutableListOf<RoutePoint>()
            snapshot.child("points").children.forEach { pointSnapshot ->
                val point = RoutePoint(
                    lat = pointSnapshot.child("lat").getValue(Double::class.java) ?: 0.0,
                    lon = pointSnapshot.child("lon").getValue(Double::class.java) ?: 0.0,
                    order = pointSnapshot.child("order").getValue(Int::class.java) ?: 0,
                    name = pointSnapshot.child("name").getValue(String::class.java) ?: ""
                )
                pointsList.add(point)
            }
            
            SafeRoute(
                id = snapshot.child("id").getValue(String::class.java) ?: "",
                name = snapshot.child("name").getValue(String::class.java) ?: "",
                description = snapshot.child("description").getValue(String::class.java) ?: "",
                points = pointsList.sortedBy { it.order },
                radius = snapshot.child("radius").getValue(Double::class.java) ?: 100.0,
                isActive = snapshot.child("isActive").getValue(Boolean::class.java) ?: true,
                createdAt = snapshot.child("createdAt").getValue(Long::class.java) ?: 0L,
                color = snapshot.child("color").getValue(String::class.java) ?: "#4CAF50"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error al parsear ruta: ${e.message}")
            null
        }
    }
    
    private fun parseSafeZone(snapshot: DataSnapshot): SafeZone? {
        return try {
            val centerSnapshot = snapshot.child("center")
            val center = Coordenadas(
                lat = centerSnapshot.child("lat").getValue(Double::class.java) ?: 0.0,
                lon = centerSnapshot.child("lon").getValue(Double::class.java) ?: 0.0
            )
            
            SafeZone(
                id = snapshot.child("id").getValue(String::class.java) ?: "",
                name = snapshot.child("name").getValue(String::class.java) ?: "",
                description = snapshot.child("description").getValue(String::class.java) ?: "",
                center = center,
                radius = snapshot.child("radius").getValue(Double::class.java) ?: 100.0,
                isActive = snapshot.child("isActive").getValue(Boolean::class.java) ?: true,
                zoneType = SafeZoneType.valueOf(snapshot.child("zoneType").getValue(String::class.java) ?: "SAFE"),
                createdAt = snapshot.child("createdAt").getValue(Long::class.java) ?: 0L,
                color = snapshot.child("color").getValue(String::class.java) ?: "#2196F3"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error al parsear zona: ${e.message}")
            null
        }
    }
    
    private fun parseAlert(snapshot: DataSnapshot): GeofenceAlert? {
        return try {
            val locationSnapshot = snapshot.child("currentLocation")
            val location = Coordenadas(
                lat = locationSnapshot.child("lat").getValue(Double::class.java) ?: 0.0,
                lon = locationSnapshot.child("lon").getValue(Double::class.java) ?: 0.0
            )
            
            GeofenceAlert(
                id = snapshot.child("id").getValue(String::class.java) ?: "",
                deviceId = snapshot.child("deviceId").getValue(String::class.java) ?: "",
                routeId = snapshot.child("routeId").getValue(String::class.java) ?: "",
                routeName = snapshot.child("routeName").getValue(String::class.java) ?: "",
                alertType = GeofenceAlertType.valueOf(snapshot.child("alertType").getValue(String::class.java) ?: "ROUTE_DEVIATION"),
                currentLocation = location,
                timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L,
                message = snapshot.child("message").getValue(String::class.java) ?: "",
                isRead = snapshot.child("isRead").getValue(Boolean::class.java) ?: false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error al parsear alerta: ${e.message}")
            null
        }
    }
} 