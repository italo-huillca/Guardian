package com.midam.guardian.presentation.screen.map
import java.util.Date
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
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
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MapScreen() {
    val deviceStatusViewModel: DeviceStatusViewModel = viewModel()
    val deviceStatus by deviceStatusViewModel.deviceStatus.collectAsState()
    val locationData by deviceStatusViewModel.locationData.collectAsState()
    
    val currentLocation = LatLng(locationData.lat, locationData.lon)
    val scope = rememberCoroutineScope()
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(currentLocation, 15f)
    }

    LaunchedEffect(locationData) {
        if (locationData.lat != 0.0 && locationData.lon != 0.0) {
            scope.launch {
                cameraPositionState.animate(
                    update = CameraUpdateFactory.newLatLng(currentLocation),
                    durationMs = 1000
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            if (locationData.lat != 0.0 && locationData.lon != 0.0) {
                Marker(
                    state = MarkerState(position = currentLocation),
                    title = "Ubicación de Guardian",
                    snippet = "Última actualización: ${formatTimestamp(deviceStatus.lastUpdateTime)}"
                )
            }
        }
        
        DeviceStatusIndicator(
            deviceStatus = deviceStatus,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        )
        
        if (deviceStatus.lastUpdateTime > 0) {
            DeviceInfoPanel(
                deviceStatus = deviceStatus,
                locationData = locationData,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
fun DeviceStatusIndicator(
    deviceStatus: DeviceStatus,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (deviceStatus.isConnected) {
        Color(0xFF4CAF50)
    } else {
        Color(0xFFF44336)
    }
    
    val statusText = if (deviceStatus.isConnected) "CONECTADO" else "DESCONECTADO"
    val statusIcon = if (deviceStatus.isConnected) Icons.Default.CheckCircle else Icons.Default.Warning
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = statusIcon,
                contentDescription = statusText,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = statusText,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun DeviceInfoPanel(
    deviceStatus: DeviceStatus,
    locationData: LocationData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Estado del Dispositivo",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Ubicación",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Lat: ${String.format("%.6f", locationData.lat)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Ubicación",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Lon: ${String.format("%.6f", locationData.lon)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Tiempo",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formatTimestamp(deviceStatus.lastUpdateTime),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Señal",
                    modifier = Modifier.size(16.dp),
                    tint = if (deviceStatus.isConnected) Color.Green else Color.Red
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = deviceStatus.signalStrength,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (deviceStatus.isConnected) Color.Green else Color.Red
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    return if (timestamp > 0) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        sdf.format(Date(timestamp))
    } else {
        "Sin datos"
    }
}
