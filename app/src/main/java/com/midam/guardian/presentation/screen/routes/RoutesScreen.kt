package com.midam.guardian.presentation.screen.routes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.midam.guardian.data.model.SafeRoute
import com.midam.guardian.data.model.GeofenceAlert
import com.midam.guardian.data.model.GeofenceAlertType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutesScreen() {
    val routesViewModel: RoutesViewModel = viewModel()
    val routes by routesViewModel.routes.collectAsState()
    val alerts by routesViewModel.alerts.collectAsState()
    val isLoading by routesViewModel.isLoading.collectAsState()
    
    var selectedTab by remember { mutableStateOf(0) }
    var showCreateRoute by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header con tabs
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Zonas Seguras",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Rutas") },
                    icon = { Icon(Icons.Default.LocationOn, "Rutas") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Alertas") },
                    icon = { 
                        BadgedBox(
                            badge = {
                                if (alerts.count { !it.isRead } > 0) {
                                    Badge { Text("${alerts.count { !it.isRead }}") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Warning, "Alertas")
                        }
                    }
                )
            }
        }
        
        // Content
        when (selectedTab) {
            0 -> RoutesTab(
                routes = routes,
                isLoading = isLoading,
                onCreateRoute = { showCreateRoute = true },
                onToggleRoute = { routeId, isActive -> 
                    routesViewModel.toggleRouteActive(routeId, isActive)
                },
                onDeleteRoute = { routeId ->
                    routesViewModel.deleteRoute(routeId)
                }
            )
            1 -> AlertsTab(
                alerts = alerts,
                onMarkAsRead = { alertId ->
                    routesViewModel.markAlertAsRead(alertId)
                }
            )
        }
    }
    
    // Create Route Dialog
    if (showCreateRoute) {
        CreateRouteDialog(
            onDismiss = { showCreateRoute = false },
            onConfirm = { name, description, points, radius, color ->
                routesViewModel.createRoute(name, description, points, radius, color)
                showCreateRoute = false
            }
        )
    }
}

@Composable
fun RoutesTab(
    routes: List<SafeRoute>,
    isLoading: Boolean,
    onCreateRoute: () -> Unit,
    onToggleRoute: (String, Boolean) -> Unit,
    onDeleteRoute: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Botón para crear ruta
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Button(
                onClick = onCreateRoute,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Add, "Crear")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Crear Nueva Ruta")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (routes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Sin rutas",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No hay rutas configuradas",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Crea una ruta para empezar a monitorear",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(routes) { route ->
                    RouteCard(
                        route = route,
                        onToggle = { isActive -> onToggleRoute(route.id, isActive) },
                        onDelete = { onDeleteRoute(route.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun RouteCard(
    route: SafeRoute,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }
    
    val defaultLocation = route.points.firstOrNull()?.toLatLng() ?: LatLng(-16.357573, -71.566635)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 15f)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = route.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (route.description.isNotEmpty()) {
                        Text(
                            text = route.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Estado
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (route.isActive) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = if (route.isActive) "ACTIVA" else "INACTIVA",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (route.isActive)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Información de la ruta
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Puntos",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${route.points.size} puntos",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = "Radio",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${route.radius.toInt()}m radio",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Fecha",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(route.createdAt)),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            // Mapa expandible
            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Mapa
                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        isMyLocationEnabled = false,
                        mapType = MapType.NORMAL
                    )
                ) {
                    // Mostrar puntos de la ruta
                    route.points.forEachIndexed { index, point ->
                        Marker(
                            state = MarkerState(position = point.toLatLng()),
                            title = point.name.ifEmpty { 
                                if (index == 0) "Inicio" 
                                else if (index == route.points.size - 1) "Destino" 
                                else "Punto ${index + 1}" 
                            }
                        )
                    }
                    
                    // Mostrar línea de la ruta
                    if (route.points.size > 1) {
                        // Dibujar el área de tolerancia primero (debajo de la línea)
                        for (i in 0 until route.points.size - 1) {
                            val start = route.points[i].toLatLng()
                            val end = route.points[i + 1].toLatLng()
                            
                            // Crear una lista de puntos para formar un polígono que represente el área de tolerancia
                            val toleranceArea = createToleranceArea(
                                start = start,
                                end = end,
                                radius = route.radius,
                                segments = 16 // número de segmentos para suavizar las curvas
                            )
                            
                            Polygon(
                                points = toleranceArea,
                                fillColor = Color(android.graphics.Color.parseColor(route.color)).copy(alpha = 0.1f),
                                strokeColor = Color(android.graphics.Color.parseColor(route.color)).copy(alpha = 0.2f),
                                strokeWidth = 2f
                            )
                        }
                        
                        // Dibujar la línea principal de la ruta
                        Polyline(
                            points = route.points.map { it.toLatLng() },
                            color = Color(android.graphics.Color.parseColor(route.color)),
                            width = 5f
                        )
                        
                        // Mostrar marcadores de inicio y fin
                        route.points.firstOrNull()?.let { startPoint ->
                            Marker(
                                state = MarkerState(position = startPoint.toLatLng()),
                                title = "Inicio",
                                snippet = route.name
                            )
                        }
                        
                        route.points.lastOrNull()?.let { endPoint ->
                            Marker(
                                state = MarkerState(position = endPoint.toLatLng()),
                                title = "Destino",
                                snippet = route.name
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Acciones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Toggle activo/inactivo
                OutlinedButton(
                    onClick = { onToggle(!route.isActive) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (route.isActive) Icons.Default.Menu else Icons.Default.PlayArrow,
                        contentDescription = if (route.isActive) "Desactivar" else "Activar",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (route.isActive) "Pausar" else "Activar")
                }
                
                // Eliminar
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar Ruta") },
            text = { Text("¿Estás seguro de que quieres eliminar la ruta '${route.name}'? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun AlertsTab(
    alerts: List<GeofenceAlert>,
    onMarkAsRead: (String) -> Unit
) {
    if (alerts.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Sin alertas",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No hay alertas",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Todas las rutas están siendo seguidas correctamente",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(alerts) { alert ->
                AlertCard(
                    alert = alert,
                    onMarkAsRead = { onMarkAsRead(alert.id) }
                )
            }
        }
    }
}

@Composable
fun AlertCard(
    alert: GeofenceAlert,
    onMarkAsRead: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = when (alert.alertType) {
                            GeofenceAlertType.ROUTE_DEVIATION -> Icons.Default.Warning
                            GeofenceAlertType.ROUTE_COMPLETED -> Icons.Default.CheckCircle
                            GeofenceAlertType.SAFE_ZONE_EXIT -> Icons.Default.ExitToApp
                            GeofenceAlertType.SAFE_ZONE_ENTER -> Icons.Default.Home
                        },
                        contentDescription = alert.alertType.name,
                        tint = when (alert.alertType) {
                            GeofenceAlertType.ROUTE_DEVIATION -> MaterialTheme.colorScheme.error
                            GeofenceAlertType.ROUTE_COMPLETED -> MaterialTheme.colorScheme.primary
                            GeofenceAlertType.SAFE_ZONE_EXIT -> MaterialTheme.colorScheme.error
                            GeofenceAlertType.SAFE_ZONE_ENTER -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (alert.alertType) {
                            GeofenceAlertType.ROUTE_DEVIATION -> "Fuera de Ruta"
                            GeofenceAlertType.ROUTE_COMPLETED -> "Ruta Completada"
                            GeofenceAlertType.SAFE_ZONE_EXIT -> "Salió de Zona Segura"
                            GeofenceAlertType.SAFE_ZONE_ENTER -> "Entró a Zona Segura"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (!alert.isRead) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.error
                    ) {
                        Text(
                            text = "NUEVO",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onError
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Información
            Text(
                text = "Ruta: ${alert.routeName}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "Dispositivo: ${alert.deviceId}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(alert.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (alert.message.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = alert.message,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            if (!alert.isRead) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onMarkAsRead,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Check, "Marcar como leída")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Marcar como Leída")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRouteDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, List<LatLng>, Double, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var radius by remember { mutableStateOf("100") }
    var selectedPoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    
    val routesViewModel: RoutesViewModel = viewModel()
    val lastLocation by routesViewModel.lastKnownLocation.collectAsState()
    
    // TECSUP Arequipa
    val tecsupLocation = LatLng(-16.430364, -71.492844)
    val defaultLocation = lastLocation ?: tecsupLocation
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 15f)
    }

    // Actualizar la posición de la cámara cuando se obtenga la última ubicación
    LaunchedEffect(lastLocation) {
        if (lastLocation != null) {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(lastLocation!!, 15f)
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crear Nueva Ruta") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(600.dp)
            ) {
                // Formulario
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de la ruta") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = radius,
                    onValueChange = { radius = it },
                    label = { Text("Radio de tolerancia (metros)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Toca en el mapa para agregar puntos de la ruta",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Mapa
                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp)),
                    cameraPositionState = cameraPositionState,
                    onMapClick = { latLng ->
                        selectedPoints = selectedPoints + latLng
                    }
                ) {
                    // Mostrar puntos seleccionados
                    selectedPoints.forEachIndexed { index, point ->
                        Marker(
                            state = MarkerState(position = point),
                            title = if (index == 0) "Inicio" else if (index == selectedPoints.size - 1) "Destino" else "Punto ${index + 1}"
                        )
                    }
                    
                    // Mostrar línea de ruta
                    if (selectedPoints.size > 1) {
                        Polyline(
                            points = selectedPoints,
                            color = Color.Blue,
                            width = 5f
                        )
                    }
                }
                
                if (selectedPoints.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${selectedPoints.size} puntos seleccionados",
                            style = MaterialTheme.typography.bodySmall
                        )
                        TextButton(
                            onClick = { selectedPoints = emptyList() }
                        ) {
                            Text("Limpiar")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && selectedPoints.size >= 2) {
                        onConfirm(
                            name,
                            description,
                            selectedPoints,
                            radius.toDoubleOrNull() ?: 100.0,
                            "#4CAF50"
                        )
                    }
                },
                enabled = name.isNotBlank() && selectedPoints.size >= 2
            ) {
                Text("Crear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// Función para crear el área de tolerancia alrededor de un segmento de ruta
private fun createToleranceArea(
    start: LatLng,
    end: LatLng,
    radius: Double,
    segments: Int
): List<LatLng> {
    val points = mutableListOf<LatLng>()
    
    // Calcular el vector de dirección del segmento
    val dx = end.longitude - start.longitude
    val dy = end.latitude - start.latitude
    val length = kotlin.math.sqrt(dx * dx + dy * dy)
    
    // Vectores normalizados
    val dirX = dx / length
    val dirY = dy / length
    
    // Vector perpendicular (normalizado)
    val perpX = -dirY
    val perpY = dirX
    
    // Factor de conversión de metros a grados (aproximado)
    // 1 grado de latitud ≈ 111,111 metros
    val metersToDegrees = radius / 111111.0
    
    // Crear puntos para el polígono
    for (i in 0..segments) {
        val angle = Math.PI * i / segments
        val sin = kotlin.math.sin(angle)
        val cos = kotlin.math.cos(angle)
        
        // Punto superior del tubo
        points.add(LatLng(
            start.latitude + metersToDegrees * (perpY * cos),
            start.longitude + metersToDegrees * (perpX * cos)
        ))
    }
    
    // Puntos para el final del segmento
    for (i in 0..segments) {
        val angle = Math.PI + Math.PI * i / segments
        val sin = kotlin.math.sin(angle)
        val cos = kotlin.math.cos(angle)
        
        // Punto inferior del tubo
        points.add(LatLng(
            end.latitude + metersToDegrees * (perpY * cos),
            end.longitude + metersToDegrees * (perpX * cos)
        ))
    }
    
    // Cerrar el polígono
    points.add(points.first())
    
    return points
} 