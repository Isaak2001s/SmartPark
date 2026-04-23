package com.example.smartpark.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.smartpark.auth.AuthUiState
import com.example.smartpark.auth.AuthViewModel
import com.example.smartpark.auth.LoginScreen
import com.example.smartpark.auth.RegisterScreen
import com.example.smartpark.home.HomeScreen
import com.example.smartpark.parking.MyReservationsScreen

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val RESERVATIONS = "reservations"
}

@Composable
fun SmartParkNavGraph(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel
) {
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val startDestination = if (uiState is AuthUiState.Authenticated) {
        Routes.HOME
    } else {
        Routes.LOGIN
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Routes.REGISTER)
                }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                viewModel = authViewModel,
                onRegisterSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                authViewModel = authViewModel,
                onLoggedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
                onNavigateToReservations = {
                    navController.navigate(Routes.RESERVATIONS)
                }
            )
        }

        composable(Routes.RESERVATIONS) {
            MyReservationsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}