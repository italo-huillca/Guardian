package com.midam.guardian.service

import android.content.Context
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.midam.guardian.data.model.HistoryLocationData
import com.midam.guardian.data.model.Coordenadas
import com.midam.guardian.data.model.UbicacionInfo
import com.midam.guardian.data.model.DetallesDireccion

interface LocationHistoryCallback {
    fun onResult(location: HistoryLocationData?)
}

object LocationHistoryWidgetService {
    private val database = FirebaseDatabase.getInstance("https://mochila-guardian.firebaseio.com")
    private val historialRef = database.getReference("historial/kid1")

    fun getLastLocation(context: Context, callback: LocationHistoryCallback) {
        historialRef.orderByKey().limitToLast(1)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var lastLocation: HistoryLocationData? = null
                    for (childSnapshot in snapshot.children) {
                        lastLocation = parseHistoryItem(childSnapshot)
                    }
                    callback.onResult(lastLocation)
                }

                override fun onCancelled(error: DatabaseError) {
                    callback.onResult(null)
                }
            })
    }

    private fun parseHistoryItem(snapshot: DataSnapshot): HistoryLocationData? {
        return try {
            val coordenadas = snapshot.child("coordenadas").let { coordSnapshot ->
                Coordenadas(
                    lat = coordSnapshot.child("lat").getValue(Double::class.java) ?: 0.0,
                    lon = coordSnapshot.child("lon").getValue(Double::class.java) ?: 0.0
                )
            }
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
            HistoryLocationData(
                device_id = snapshot.child("device_id").getValue(String::class.java) ?: "kid1",
                timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L,
                fecha_legible = snapshot.child("fecha_legible").getValue(String::class.java) ?: "Fecha no disponible",
                coordenadas = coordenadas,
                ubicacion = ubicacion
            )
        } catch (e: Exception) {
            null
        }
    }
} 