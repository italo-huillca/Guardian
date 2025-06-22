package com.midam.guardian.data.model

import com.google.android.gms.maps.model.LatLng

data class SafeRoute(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val points: List<RoutePoint> = emptyList(),
    val radius: Double = 100.0, // Radio en metros para considerar "dentro de la ruta"
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val color: String = "#4CAF50" // Color para mostrar en el mapa
)

data class RoutePoint(
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val order: Int = 0,
    val name: String = "" // Nombre opcional del punto (ej. "Casa", "Escuela")
) {
    fun toLatLng(): LatLng = LatLng(lat, lon)
}

data class GeofenceAlert(
    val id: String = "",
    val deviceId: String = "",
    val routeId: String = "",
    val routeName: String = "",
    val alertType: GeofenceAlertType = GeofenceAlertType.ROUTE_DEVIATION,
    val currentLocation: Coordenadas = Coordenadas(),
    val timestamp: Long = System.currentTimeMillis(),
    val message: String = "",
    val isRead: Boolean = false
)

enum class GeofenceAlertType {
    ROUTE_DEVIATION,     // Se salió de la ruta
    ROUTE_COMPLETED,     // Completó la ruta exitosamente
    SAFE_ZONE_EXIT,      // Salió de zona segura
    SAFE_ZONE_ENTER      // Entró a zona segura
}

data class SafeZone(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val center: Coordenadas = Coordenadas(),
    val radius: Double = 100.0, // Radio en metros
    val isActive: Boolean = true,
    val zoneType: SafeZoneType = SafeZoneType.SAFE,
    val createdAt: Long = System.currentTimeMillis(),
    val color: String = "#2196F3"
)

enum class SafeZoneType {
    SAFE,        // Zona segura (casa, escuela)
    RESTRICTED,  // Zona restringida (lugares peligrosos)
    CHECKPOINT   // Punto de control (debe pasar por aquí)
}

data class RouteProgress(
    val deviceId: String = "",
    val routeId: String = "",
    val currentPointIndex: Int = 0,
    val completedPoints: List<Int> = emptyList(),
    val startTime: Long = 0L,
    val estimatedArrival: Long = 0L,
    val isActive: Boolean = false
) 