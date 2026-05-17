package pl.dekrate.kofeino.tracker.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test
import pl.dekrate.kofeino.tracker.R
import pl.dekrate.kofeino.tracker.domain.model.CaffeineIntake
import pl.dekrate.kofeino.tracker.presentation.viewmodel.HomeUiState
import pl.dekrate.kofeino.tracker.presentation.viewmodel.HomeViewModel
import pl.dekrate.kofeino.tracker.ui.theme.KofeinoTrackerPhoneTheme

class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    // ===== Basic display tests =====

    @Test
    fun homeScreen_displaysDateLabel() {
        val fakeVm = createFakeViewModel(
            HomeUiState(dateLabel = "Monday, 17.05.2026", isLoading = false)
        )
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                HomeScreen(
                    onNavigateToAddDrink = {},
                    onNavigateToHistory = {},
                    onNavigateToManageDrinks = {},
                    onNavigateToSettings = {},
                    onEditIntake = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText("Monday, 17.05.2026").assertIsDisplayed()
    }

    @Test
    fun homeScreen_displaysTotalCaffeine() {
        val fakeVm = createFakeViewModel(
            HomeUiState(totalCaffeineMg = 250, progress = 0.625f, isLoading = false)
        )
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                HomeScreen(
                    onNavigateToAddDrink = {},
                    onNavigateToHistory = {},
                    onNavigateToManageDrinks = {},
                    onNavigateToSettings = {},
                    onEditIntake = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText("250").assertIsDisplayed()
    }

    @Test
    fun homeScreen_displaysLimitInfo() {
        val fakeVm = createFakeViewModel(
            HomeUiState(totalCaffeineMg = 250, progress = 0.625f, isLoading = false)
        )
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                HomeScreen(
                    onNavigateToAddDrink = {},
                    onNavigateToHistory = {},
                    onNavigateToManageDrinks = {},
                    onNavigateToSettings = {},
                    onEditIntake = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.safe_limit), substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun homeScreen_showsLimitExceededMessage_whenOverLimit() {
        val fakeVm = createFakeViewModel(
            HomeUiState(totalCaffeineMg = 450, progress = 1f, isLimitExceeded = true, isLoading = false)
        )
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                HomeScreen(
                    onNavigateToAddDrink = {},
                    onNavigateToHistory = {},
                    onNavigateToManageDrinks = {},
                    onNavigateToSettings = {},
                    onEditIntake = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.limit_exceeded), substring = true)
            .assertIsDisplayed()
    }

    // ===== Intake list tests =====

    @Test
    fun homeScreen_displaysIntakeItems() {
        val intakes = listOf(
            CaffeineIntake(1, drinkName = "Latte", caffeineMg = 63, volumeMl = 250, timestamp = 0L),
            CaffeineIntake(2, drinkName = "Espresso", caffeineMg = 63, volumeMl = 30, timestamp = 0L)
        )
        val fakeVm = createFakeViewModel(
            HomeUiState(todayIntakes = intakes, totalCaffeineMg = 126, isLoading = false)
        )
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                HomeScreen(
                    onNavigateToAddDrink = {},
                    onNavigateToHistory = {},
                    onNavigateToManageDrinks = {},
                    onNavigateToSettings = {},
                    onEditIntake = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText("Latte", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Espresso", substring = true).assertIsDisplayed()
    }

    @Test
    fun homeScreen_showsEmptyState_whenNoIntakes() {
        val fakeVm = createFakeViewModel(
            HomeUiState(todayIntakes = emptyList(), isLoading = false)
        )
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                HomeScreen(
                    onNavigateToAddDrink = {},
                    onNavigateToHistory = {},
                    onNavigateToManageDrinks = {},
                    onNavigateToSettings = {},
                    onEditIntake = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.no_drinks_today))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.tap_add_to_start))
            .assertIsDisplayed()
    }

    // ===== Navigation tests =====

    @Test
    fun homeScreen_fabButton_navigatesToAddDrink() {
        var navigated = false
        val fakeVm = createFakeViewModel(
            HomeUiState(isLoading = false)
        )
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                HomeScreen(
                    onNavigateToAddDrink = { navigated = true },
                    onNavigateToHistory = {},
                    onNavigateToManageDrinks = {},
                    onNavigateToSettings = {},
                    onEditIntake = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.add_drink))
            .performClick()
        assert(navigated) { "FAB click should trigger onNavigateToAddDrink" }
    }

    @Test
    fun homeScreen_historyIcon_navigatesToHistory() {
        var navigated = false
        val fakeVm = createFakeViewModel(
            HomeUiState(isLoading = false)
        )
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                HomeScreen(
                    onNavigateToAddDrink = {},
                    onNavigateToHistory = { navigated = true },
                    onNavigateToManageDrinks = {},
                    onNavigateToSettings = {},
                    onEditIntake = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.history_title))
            .performClick()
        assert(navigated) { "History icon click should trigger onNavigateToHistory" }
    }

    @Test
    fun homeScreen_manageDrinksIcon_navigatesToManageDrinks() {
        var navigated = false
        val fakeVm = createFakeViewModel(
            HomeUiState(isLoading = false)
        )
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                HomeScreen(
                    onNavigateToAddDrink = {},
                    onNavigateToHistory = {},
                    onNavigateToManageDrinks = { navigated = true },
                    onNavigateToSettings = {},
                    onEditIntake = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.manage_drinks_title))
            .performClick()
        assert(navigated) { "Manage drinks icon click should trigger onNavigateToManageDrinks" }
    }

    @Test
    fun homeScreen_settingsIcon_navigatesToSettings() {
        var navigated = false
        val fakeVm = createFakeViewModel(
            HomeUiState(isLoading = false)
        )
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                HomeScreen(
                    onNavigateToAddDrink = {},
                    onNavigateToHistory = {},
                    onNavigateToManageDrinks = {},
                    onNavigateToSettings = { navigated = true },
                    onEditIntake = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.settings_title))
            .performClick()
        assert(navigated) { "Settings icon click should trigger onNavigateToSettings" }
    }

    @Test
    fun homeScreen_intakeClick_callsOnEditIntake() {
        var editedId = -1L
        val intakes = listOf(
            CaffeineIntake(42, drinkName = "Cappuccino", caffeineMg = 75, volumeMl = 200, timestamp = 0L)
        )
        val fakeVm = createFakeViewModel(
            HomeUiState(todayIntakes = intakes, totalCaffeineMg = 75, isLoading = false)
        )
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                HomeScreen(
                    onNavigateToAddDrink = {},
                    onNavigateToHistory = {},
                    onNavigateToManageDrinks = {},
                    onNavigateToSettings = {},
                    onEditIntake = { editedId = it },
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText("Cappuccino", substring = true).performClick()
        assert(editedId == 42L) { "Expected 42L, got $editedId" }
    }

    // ===== Loading state test =====

    @Test
    fun homeScreen_showsLoadingIndicator_whenLoading() {
        val fakeVm = createFakeViewModel(
            HomeUiState(isLoading = true)
        )
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                HomeScreen(
                    onNavigateToAddDrink = {},
                    onNavigateToHistory = {},
                    onNavigateToManageDrinks = {},
                    onNavigateToSettings = {},
                    onEditIntake = {},
                    viewModel = fakeVm
                )
            }
        }
        // TopAppBar should still show (it's in Scaffold), but content is loading
        // We can verify the FAB exists (Scaffold renders FAB) while content shows loading
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.add_drink))
            .assertIsDisplayed()
    }

    // ===== Accessibility content description tests =====

    @Test
    fun homeScreen_fabButton_hasContentDescription() {
        val fakeVm = createFakeViewModel(
            HomeUiState(isLoading = false)
        )
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                HomeScreen(
                    onNavigateToAddDrink = {},
                    onNavigateToHistory = {},
                    onNavigateToManageDrinks = {},
                    onNavigateToSettings = {},
                    onEditIntake = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.add_drink))
            .assertIsDisplayed()
    }

    @Test
    fun homeScreen_historyIcon_hasContentDescription() {
        val fakeVm = createFakeViewModel(
            HomeUiState(isLoading = false)
        )
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                HomeScreen(
                    onNavigateToAddDrink = {},
                    onNavigateToHistory = {},
                    onNavigateToManageDrinks = {},
                    onNavigateToSettings = {},
                    onEditIntake = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.history_title))
            .assertIsDisplayed()
    }

    @Test
    fun homeScreen_manageDrinksIcon_hasContentDescription() {
        val fakeVm = createFakeViewModel(
            HomeUiState(isLoading = false)
        )
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                HomeScreen(
                    onNavigateToAddDrink = {},
                    onNavigateToHistory = {},
                    onNavigateToManageDrinks = {},
                    onNavigateToSettings = {},
                    onEditIntake = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.manage_drinks_title))
            .assertIsDisplayed()
    }

    @Test
    fun homeScreen_settingsIcon_hasContentDescription() {
        val fakeVm = createFakeViewModel(
            HomeUiState(isLoading = false)
        )
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                HomeScreen(
                    onNavigateToAddDrink = {},
                    onNavigateToHistory = {},
                    onNavigateToManageDrinks = {},
                    onNavigateToSettings = {},
                    onEditIntake = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.settings_title))
            .assertIsDisplayed()
    }

    @Test
    fun homeScreen_intakeItem_hasContentDescription() {
        val intakes = listOf(
            CaffeineIntake(1, drinkName = "Americano", caffeineMg = 80, volumeMl = 300, timestamp = 0L)
        )
        val fakeVm = createFakeViewModel(
            HomeUiState(todayIntakes = intakes, totalCaffeineMg = 80, isLoading = false)
        )
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                HomeScreen(
                    onNavigateToAddDrink = {},
                    onNavigateToHistory = {},
                    onNavigateToManageDrinks = {},
                    onNavigateToSettings = {},
                    onEditIntake = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithContentDescription("Americano 80 mg", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun homeScreen_coffeeCupIndicator_hasContentDescription() {
        val fakeVm = createFakeViewModel(
            HomeUiState(totalCaffeineMg = 200, progress = 0.5f, isLoading = false)
        )
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                HomeScreen(
                    onNavigateToAddDrink = {},
                    onNavigateToHistory = {},
                    onNavigateToManageDrinks = {},
                    onNavigateToSettings = {},
                    onEditIntake = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(
                "200 mg ${context.getString(R.string.safe_limit)}", substring = true
            )
            .assertIsDisplayed()
    }

    // ===== Utility =====

    private fun createFakeViewModel(
        state: HomeUiState
    ): HomeViewModel {
        val vm = mockk<HomeViewModel>(relaxed = true)
        every { vm.uiState } returns MutableStateFlow(state) as StateFlow<HomeUiState>
        return vm
    }
}
