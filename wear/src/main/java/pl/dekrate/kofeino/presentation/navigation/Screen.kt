package pl.dekrate.kofeino.presentation.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object AddDrink : Screen("add_drink")
    data object History : Screen("history")
    data object ManageDrinks : Screen("manage_drinks")
    data object OfficialDrinks : Screen("official_drinks")
    data object EditIntake : Screen("edit_intake/{intakeId}")
    data object Settings : Screen("settings")
    data object CrossDeviceStatus : Screen("cross_device_status")

    companion object {
        const val ARG_INTAKE_ID = "intakeId"
        fun editIntake(intakeId: Long) = "edit_intake/$intakeId"
    }
}
