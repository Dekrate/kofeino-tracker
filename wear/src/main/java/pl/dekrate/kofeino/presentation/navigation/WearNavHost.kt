package pl.dekrate.kofeino.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import pl.dekrate.kofeino.presentation.screens.AddDrinkScreen
import pl.dekrate.kofeino.presentation.screens.EditIntakeScreen
import pl.dekrate.kofeino.presentation.screens.HistoryScreen
import pl.dekrate.kofeino.presentation.screens.HomeScreen
import pl.dekrate.kofeino.presentation.screens.ManageDrinksScreen

@Composable
fun WearNavHost(navController: NavHostController) {
    SwipeDismissableNavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToAddDrink = { navController.navigate(Screen.AddDrink.route) },
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onNavigateToManageDrinks = { navController.navigate(Screen.ManageDrinks.route) },
                onEditIntake = { intakeId ->
                    navController.navigate(Screen.editIntake(intakeId))
                }
            )
        }
        composable(Screen.AddDrink.route) {
            AddDrinkScreen(
                onDrinkAdded = { navController.popBackStack() }
            )
        }
        composable(Screen.History.route) {
            HistoryScreen(
                onEditIntake = { intakeId ->
                    navController.navigate(Screen.editIntake(intakeId))
                }
            )
        }
        composable(Screen.ManageDrinks.route) {
            ManageDrinksScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.EditIntake.route,
            arguments = listOf(
                navArgument(Screen.ARG_INTAKE_ID) { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val intakeId = backStackEntry.arguments?.getLong(Screen.ARG_INTAKE_ID) ?: 0L
            EditIntakeScreen(
                intakeId = intakeId,
                onDeleted = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
    }
}
