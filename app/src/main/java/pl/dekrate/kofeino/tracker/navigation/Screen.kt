package pl.dekrate.kofeino.tracker.navigation

/**
 * Navigation routes for the phone app — mirrors :wear destinations 1:1.
 */
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object AddDrink : Screen("add_drink")
    data object History : Screen("history")
    data object ManageDrinks : Screen("manage_drinks")
    data object OfficialDrinks : Screen("official_drinks")
    data object EditIntake : Screen("edit_intake/{intakeId}")
    data object Settings : Screen("settings")

    companion object {
        const val ARG_INTAKE_ID = "intakeId"
        fun editIntake(intakeId: Long) = "edit_intake/$intakeId"
    }
}
