package com.midam.guardian.presentation.screen.splash

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.*
import androidx.compose.ui.tooling.preview.*
import com.midam.guardian.R

@Composable
fun SplashScreen() {
    Splash()
}

@Composable
fun Splash() {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(modifier = Modifier.fillMaxSize(), painter = painterResource(id = R.drawable.logo), contentDescription = "Logo guardian")
    }
}

@Preview
@Composable
fun SplashPreview() {
    Splash()
}