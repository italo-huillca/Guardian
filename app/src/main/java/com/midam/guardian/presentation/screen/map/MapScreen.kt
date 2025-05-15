package com.midam.guardian.presentation.screen.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.type.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MapScreen() {
    var currentLocation by remember { mutableStateOf(LatLng(-16.431273, -71.518135)) }
    val scope = rememberCoroutineScope()
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(currentLocation, 15f)
    }

    LaunchedEffect(Unit) {
        val database = FirebaseDatabase.getInstance("https://mochila-guardian.firebaseio.com")
        val ubicacionesRef = database.getReference("ubicaciones/kid1")

        ubicacionesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lat = snapshot.child("lat").getValue(Double::class.java) ?: -16.431273
                val lon = snapshot.child("lon").getValue(Double::class.java) ?: -71.518135
                currentLocation = LatLng(lat, lon)
                scope.launch {
                    cameraPositionState.animate(
                        update = CameraUpdateFactory.newLatLng(currentLocation),
                        durationMs = 1000
                    )
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Manejar error si es necesario
            }
        })
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        Marker(
            state = MarkerState(position = currentLocation),
            title = "Ubicación actual"
        )
    }
}
