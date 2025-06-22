package com.midam.guardian.data.model

data class HistoryLocationData(
    val device_id: String = "",
    val timestamp: Long = 0L,
    val fecha_legible: String = "",
    val coordenadas: Coordenadas = Coordenadas(),
    val ubicacion: UbicacionInfo = UbicacionInfo()
)

data class Coordenadas(
    val lat: Double = 0.0,
    val lon: Double = 0.0
)

data class UbicacionInfo(
    val direccion_completa: String = "",
    val detalles: DetallesDireccion = DetallesDireccion()
)

data class DetallesDireccion(
    val calle: String = "",
    val numero: String = "",
    val barrio: String = "",
    val ciudad: String = "",
    val estado: String = "",
    val pais: String = ""
) 