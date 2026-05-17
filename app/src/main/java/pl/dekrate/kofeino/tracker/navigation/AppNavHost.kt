package pl.dekrate.kofeino.tracker.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import pl.dekrate.kofeino.tracker.ui.screens.AddDrinkScreen
import pl.dekrate.kofeino.tracker.ui.screens.EditIntakeScreen
import pl.dekrate.kofeino.tracker.ui.screens.HistoryScreen
import pl.dekrate.kofeino.tracker.ui.screens.HomeScreen
import pl.dekrate.kofeino.tracker.ui.screens.ManageDrinksScreen
import pl.dekrate.kofeino.tracker.ui.screens.OfficialDrinksScreen
import pl.dekrate.kofeino.tracker.ui.screens.SettingsScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToAddDrink = { navController.navigate(Screen.AddDrink.route) },
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onNavigateToManageDrinks = { navController.navigate(Screen.ManageDrinks.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onEditIntake = { intakeId ->
                    navController.navigate(Screen.editIntake(intakeId))
                }
            )
        }
        composable(Screen.AddDrink.route) {
            AddDrinkScreen(
                onNavigateBack = { navController.popBackStack() }
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
                onNavigateToOfficialDrinks = { navController.navigate(Screen.OfficialDrinks.route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.OfficialDrinks.route) {
            OfficialDrinksScreen(
                onNavigateBack = { navController.popBackStack() }
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
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
