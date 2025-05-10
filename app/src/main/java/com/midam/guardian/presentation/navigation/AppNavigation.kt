package com.midam.guardian.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.midam.guardian.presentation.screen.main.MainScreen
import com.midam.guardian.presentation.screen.splash.SplashScreen
import com.midam.guardian.presentation.screen.login.LoginScreen
import com.midam.guardian.presentation.screen.register.RegisterScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = SplashScreen.route) {
        composable(SplashScreen.route) {
            SplashScreen {
                navController.navigate(LoginScreen.route) {
                    popUpTo(SplashScreen.route) { inclusive = true }
                }
            }
        }
        composable(MainScreen.route) {
            MainScreen()
        }
        composable(LoginScreen.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(MainScreen.route) {
                        popUpTo(LoginScreen.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(RegisterScreen.route)
                }
            )
        }
        composable(RegisterScreen.route) {
            RegisterScreen {
                navController.navigate(LoginScreen.route) {
                    popUpTo(RegisterScreen.route) { inclusive = true }
                }
            }
        }
    }
}