package com.midam.guardian.presentation.screen.splash

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.midam.guardian.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navigationToMain: () -> Unit) {
    LaunchedEffect(key1 = true) {
        delay(2000)
        navigationToMain()
    }
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
        Image(
            modifier = Modifier.fillMaxSize(),
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Logo guardian"
        )
    }
}

@Preview
@Composable
fun SplashPreview() {
    Splash()
}