package com.example.kofeinotracker.presentation.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object AddDrink : Screen("add_drink")
    data object History : Screen("history")
}
