package pl.dekrate.kofeino.presentation.screens

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import pl.dekrate.kofeino.R
import pl.dekrate.kofeino.domain.model.DrinkEntity
import pl.dekrate.kofeino.presentation.theme.KofeinoTrackerTheme
import pl.dekrate.kofeino.presentation.viewmodel.CaffeineUiState
import pl.dekrate.kofeino.presentation.viewmodel.CaffeineViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun homeScreen_displaysTitle() {
        val fakeViewModel = createFakeViewModel(CaffeineUiState())
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                HomeScreen(
                    onNavigateToAddDrink = {},
                    onNavigateToHistory = {},
                    onNavigateToManageDrinks = {},
                    onEditIntake = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.today_caffeine)).assertIsDisplayed()
    }

    @Test
    fun homeScreen_displaysTotalCaffeine() {
        val fakeViewModel = createFakeViewModel(CaffeineUiState(totalCaffeineMg = 150))
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                HomeScreen(
                    onNavigateToAddDrink = {},
                    onNavigateToHistory = {},
                    onNavigateToManageDrinks = {},
                    onEditIntake = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithText("150").assertIsDisplayed()
    }

    @Test
    fun homeScreen_displaysLimitExceededWarning() {
        val fakeViewModel = createFakeViewModel(
            CaffeineUiState(totalCaffeineMg = 450, isLimitExceeded = true)
        )
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                HomeScreen(
                    onNavigateToAddDrink = {},
                    onNavigateToHistory = {},
                    onNavigateToManageDrinks = {},
                    onEditIntake = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.limit_exceeded)).assertIsDisplayed()
    }

    @Test
    fun homeScreen_displaysIntakeList() {
        val intakes = listOf(
            pl.dekrate.kofeino.domain.model.CaffeineIntake(1, drinkName = "Espresso", caffeineMg = 63, volumeMl = 30, timestamp = 0L),
            pl.dekrate.kofeino.domain.model.CaffeineIntake(2, drinkName = "Latte", caffeineMg = 63, volumeMl = 250, timestamp = 0L)
        )
        val fakeViewModel = createFakeViewModel(CaffeineUiState(dateIntakes = intakes))
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                HomeScreen(
                    onNavigateToAddDrink = {},
                    onNavigateToHistory = {},
                    onNavigateToManageDrinks = {},
                    onEditIntake = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onAllNodesWithText("Espresso", substring = true).assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Latte", substring = true).assertCountEquals(1)
    }

    @Test
    fun homeScreen_showsEmptyMessage_whenNoIntakes() {
        val fakeViewModel = createFakeViewModel(CaffeineUiState(dateIntakes = emptyList()))
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                HomeScreen(
                    onNavigateToAddDrink = {},
                    onNavigateToHistory = {},
                    onNavigateToManageDrinks = {},
                    onEditIntake = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.no_drinks_today)).assertIsDisplayed()
    }

    @Test
    fun homeScreen_navigatesToAddDrink_whenPlusClicked() {
        var clicked = false
        val fakeViewModel = createFakeViewModel(CaffeineUiState())
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                HomeScreen(
                    onNavigateToAddDrink = { clicked = true },
                    onNavigateToHistory = {},
                    onNavigateToManageDrinks = {},
                    onEditIntake = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithText("+").assertIsDisplayed().performClick()
        assert(clicked)
    }

    @Test
    fun homeScreen_navigatesToHistory_whenHistoryClicked() {
        var clicked = false
        val fakeViewModel = createFakeViewModel(CaffeineUiState())
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                HomeScreen(
                    onNavigateToAddDrink = {},
                    onNavigateToHistory = { clicked = true },
                    onNavigateToManageDrinks = {},
                    onEditIntake = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.history)).assertIsDisplayed().performClick()
        assert(clicked)
    }

    @Test
    fun homeScreen_showsDateLabel() {
        val fakeViewModel = createFakeViewModel(CaffeineUiState(dateLabel = context.getString(R.string.today)))
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                HomeScreen(
                    onNavigateToAddDrink = {},
                    onNavigateToHistory = {},
                    onNavigateToManageDrinks = {},
                    onNavigateToSettings = {},
                    onEditIntake = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.today)).assertIsDisplayed()
    }

    @Test
    fun homeScreen_showsManageDrinksButton() {
        var clicked = false
        val fakeViewModel = createFakeViewModel(CaffeineUiState())
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                HomeScreen(
                    onNavigateToAddDrink = {},
                    onNavigateToHistory = {},
                    onNavigateToManageDrinks = { clicked = true },
                    onEditIntake = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.manage_drinks)).assertIsDisplayed().performClick()
        assert(clicked)
    }

    @Test
    fun homeScreen_intakeClick_callsOnEditIntake() {
        var editedId = -1L
        val intakes = listOf(
            pl.dekrate.kofeino.domain.model.CaffeineIntake(42, drinkName = "Test", caffeineMg = 50, volumeMl = 200, timestamp = 0L)
        )
        val fakeViewModel = createFakeViewModel(CaffeineUiState(dateIntakes = intakes))
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                HomeScreen(
                    onNavigateToAddDrink = {},
                    onNavigateToHistory = {},
                    onNavigateToManageDrinks = {},
                    onEditIntake = { editedId = it },
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithText("Test", substring = true).performClick()
        assert(editedId == 42L)
    }

    // ===== Accessibility content description tests =====

    @Test
    fun homeScreen_addDrinkButton_hasContentDescription() {
        val fakeViewModel = createFakeViewModel(CaffeineUiState())
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                HomeScreen(
                    onNavigateToAddDrink = {},
                    onNavigateToHistory = {},
                    onNavigateToManageDrinks = {},
                    onEditIntake = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.add_drink))
            .assertIsDisplayed()
    }

    @Test
    fun homeScreen_historyButton_hasContentDescription() {
        val fakeViewModel = createFakeViewModel(CaffeineUiState())
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                HomeScreen(
                    onNavigateToAddDrink = {},
                    onNavigateToHistory = {},
                    onNavigateToManageDrinks = {},
                    onEditIntake = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.history))
            .assertIsDisplayed()
    }

    @Test
    fun homeScreen_settingsButton_hasContentDescription() {
        val fakeViewModel = createFakeViewModel(CaffeineUiState())
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                HomeScreen(
                    onNavigateToAddDrink = {},
                    onNavigateToHistory = {},
                    onNavigateToManageDrinks = {},
                    onNavigateToSettings = {},
                    onEditIntake = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.settings))
            .assertIsDisplayed()
    }

    private fun createFakeViewModel(state: CaffeineUiState): CaffeineViewModel {
        val vm = mockk<CaffeineViewModel>(relaxed = true)
        every { vm.uiState } returns MutableStateFlow(state) as StateFlow<CaffeineUiState>
        return vm
    }
}
