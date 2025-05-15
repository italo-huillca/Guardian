package com.midam.guardian.presentation.screen.main

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*

@Composable
fun MainScreen(
    onNavigateToMap: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "Pantalla principal", fontSize = 32.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onNavigateToMap,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("Ver Mapa en Tiempo Real")
        }
    }
}