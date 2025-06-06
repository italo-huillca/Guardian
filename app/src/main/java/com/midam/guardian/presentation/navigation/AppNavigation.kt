package com.midam.guardian.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.midam.guardian.data.auth.AuthManager
import com.midam.guardian.presentation.screen.main.MainScreen
import com.midam.guardian.presentation.screen.splash.SplashScreen
import com.midam.guardian.presentation.screen.login.LoginScreen
import com.midam.guardian.presentation.screen.register.RegisterScreen
import com.midam.guardian.presentation.screen.settings.SettingsScreen
import com.midam.guardian.presentation.screen.notifications.NotificationsScreen
import com.midam.guardian.presentation.screen.notifications.NotificationsViewModel

@Composable
fun AppNavigation(notificationsViewModel: NotificationsViewModel) {
    val context = LocalContext.current
    val authRepository = remember { AuthManager.getInstance(context) }
    val isAuthenticated by authRepository.isAuthenticated.collectAsState()
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = SplashScreen.route) {
        composable(SplashScreen.route) {
            SplashScreen {
                if (isAuthenticated) {
                    navController.navigate(MainScreen.route) {
                        popUpTo(SplashScreen.route) { inclusive = true }
                    }
                } else {
                    navController.navigate(LoginScreen.route) {
                        popUpTo(SplashScreen.route) { inclusive = true }
                    }
                }
            }
        }
        composable(MainScreen.route) {
            MainScreen(
                onNavigateToSettings = {
                    navController.navigate(SettingsScreen.route)
                },
                onNavigateToNotifications = {
                    navController.navigate(NotificationsScreen.route)
                },
                notificationsViewModel = notificationsViewModel
            )
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
        composable(SettingsScreen.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLogout = {
                    navController.navigate(LoginScreen.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(NotificationsScreen.route) {
            NotificationsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                viewModel = notificationsViewModel
            )
        }
    }
}