package com.midam.guardian.presentation.navigation

import androidx.compose.runtime.*
import androidx.navigation.*
import androidx.navigation.NavHost
import androidx.navigation.compose.*
import com.midam.guardian.presentation.screen.main.*
import com.midam.guardian.presentation.screen.splash.*

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = AppScreens.SplashScreen.route){
        composable(AppScreens.SplashScreen.route){
            SplashScreen(navController)
        }
        composable(AppScreens.MainScreen.route){
            MainScreen()
        }
    }
}