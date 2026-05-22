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
import pl.dekrate.kofeino.presentation.screens.OfficialDrinksScreen
import pl.dekrate.kofeino.presentation.screens.SettingsScreen
import pl.dekrate.kofeino.presentation.viewmodel.DrinkViewModel
import pl.dekrate.kofeino.presentation.viewmodel.SettingsViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import timber.log.Timber

@Composable
fun WearNavHost(navController: NavHostController) {
    val drinkViewModel: DrinkViewModel = hiltViewModel()

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = Screen.Home.route
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
                onOfficialDrinks = { navController.navigate(Screen.OfficialDrinks.route) }
            )
        }
        composable(Screen.OfficialDrinks.route) {
            OfficialDrinksScreen(
                onDrinkSelected = { drink ->
                    Timber.d("Importing official drink: ${drink.name} (${drink.caffeineMgPer100ml} mg/100ml)")
                    drinkViewModel.addDrink(
                        name = drink.name,
                        caffeineMg = kotlin.math.round(drink.caffeineMgPer100ml).toInt(),
                        volumeMl = 100
                    )
                    navController.popBackStack()
                }
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
        composable(Screen.Settings.route) {
            SettingsScreen(viewModel = hiltViewModel())
        }
    }
}
