package com.example.kofeinotracker.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import com.example.kofeinotracker.presentation.screens.AddDrinkScreen
import com.example.kofeinotracker.presentation.screens.HistoryScreen
import com.example.kofeinotracker.presentation.screens.HomeScreen

@Composable
fun WearNavHost(navController: NavHostController) {
    SwipeDismissableNavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToAddDrink = { navController.navigate(Screen.AddDrink.route) },
                onNavigateToHistory = { navController.navigate(Screen.History.route) }
            )
        }
        composable(Screen.AddDrink.route) {
            AddDrinkScreen(
                onDrinkAdded = { navController.popBackStack() }
            )
        }
        composable(Screen.History.route) {
            HistoryScreen()
        }
    }
}
