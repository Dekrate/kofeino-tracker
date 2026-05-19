package pl.dekrate.kofeino.tracker.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pl.dekrate.kofeino.tracker.R
import pl.dekrate.kofeino.tracker.domain.model.DrinkEntity
import pl.dekrate.kofeino.tracker.presentation.viewmodel.DrinkUiState
import pl.dekrate.kofeino.tracker.presentation.viewmodel.DrinkViewModel
import pl.dekrate.kofeino.tracker.ui.theme.KofeinoTrackerPhoneTheme

class AddDrinkScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun addDrinkScreen_displaysTitle() {
        val fakeViewModel = createFakeViewModel(emptyList())
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                AddDrinkScreen(
                    onNavigateBack = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.select_drink)).assertIsDisplayed()
    }

    @Test
    fun addDrinkScreen_displaysAllPredefinedDrinks() {
        val drinks = listOf(
            DrinkEntity(1, "Espresso", 63, 30, true),
            DrinkEntity(2, "Cappuccino", 75, 200, true),
            DrinkEntity(3, "Czarna kawa", 95, 250, true),
            DrinkEntity(4, "Energy drink", 80, 250, true)
        )
        val fakeViewModel = createFakeViewModel(drinks)
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                AddDrinkScreen(
                    onNavigateBack = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithText("Espresso").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cappuccino").assertIsDisplayed()
        composeTestRule.onNodeWithText("Czarna kawa").assertIsDisplayed()
        composeTestRule.onNodeWithText("Energy drink").assertIsDisplayed()
    }

    @Test
    fun addDrinkScreen_clickDrink_showsConfirmation() {
        val drinks = listOf(
            DrinkEntity(1, "Espresso", 63, 30, true)
        )
        val fakeViewModel = createFakeViewModel(drinks)
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                AddDrinkScreen(
                    onNavigateBack = {},
                    viewModel = fakeViewModel
                )
            }
        }

        // Tap the drink
        composeTestRule.onNodeWithText("Espresso").performClick()

        // Confirmation should show drink name, adjust label, steppers, buttons
        composeTestRule.onNodeWithText("Espresso").assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.adjust_serving)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.caffeine_label, 63)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.volume_label, 30)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.log_drink)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.cancel)).assertIsDisplayed()

        // Title should now be "Adjust serving", not "Select drink"
        composeTestRule.onNodeWithText(context.getString(R.string.select_drink)).assertIsNotDisplayed()
    }

    @Test
    fun addDrinkScreen_confirmation_cancel_returnsToList() {
        val drinks = listOf(
            DrinkEntity(1, "Espresso", 63, 30, true)
        )
        val fakeViewModel = createFakeViewModel(drinks)
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                AddDrinkScreen(
                    onNavigateBack = {},
                    viewModel = fakeViewModel
                )
            }
        }

        // Tap the drink -> confirmation shown
        composeTestRule.onNodeWithText("Espresso").performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.adjust_serving)).assertIsDisplayed()

        // Tap Cancel
        composeTestRule.onNodeWithText(context.getString(R.string.cancel)).performClick()

        // List should be back
        composeTestRule.onNodeWithText(context.getString(R.string.select_drink)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.adjust_serving)).assertIsNotDisplayed()
    }

    @Test
    fun addDrinkScreen_confirmation_caffeineFineStepper_adjustsValue() {
        val drinks = listOf(
            DrinkEntity(1, "Espresso", 63, 30, true)
        )
        val fakeViewModel = createFakeViewModel(drinks)
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                AddDrinkScreen(
                    onNavigateBack = {},
                    viewModel = fakeViewModel
                )
            }
        }

        // Tap the drink -> confirmation shown
        composeTestRule.onNodeWithText("Espresso").performClick()

        // Default caffeine value
        composeTestRule.onNodeWithText(
            context.getString(R.string.caffeine_label, 63)
        ).assertIsDisplayed()

        // Tap +1 -> 64
        composeTestRule.onNodeWithText(
            context.getString(R.string.caffeine_adjustment_increase, 1)
        ).performClick()
        composeTestRule.onNodeWithText(
            context.getString(R.string.caffeine_label, 64)
        ).assertIsDisplayed()

        // Tap -1 twice -> 62
        composeTestRule.onNodeWithText(
            context.getString(R.string.caffeine_adjustment_decrease, 1)
        ).performClick()
        composeTestRule.onNodeWithText(
            context.getString(R.string.caffeine_adjustment_decrease, 1)
        ).performClick()
        composeTestRule.onNodeWithText(
            context.getString(R.string.caffeine_label, 62)
        ).assertIsDisplayed()
    }

    @Test
    fun addDrinkScreen_confirmation_caffeineFineStepper_minimumGuard() {
        val drinks = listOf(
            DrinkEntity(1, "Espresso", 3, 30, true)
        )
        val fakeViewModel = createFakeViewModel(drinks)
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                AddDrinkScreen(
                    onNavigateBack = {},
                    viewModel = fakeViewModel
                )
            }
        }

        // Tap the drink -> confirmation shown (3 mg caffeine)
        composeTestRule.onNodeWithText("Espresso").performClick()

        // Tap -1 three times -> 0
        repeat(3) {
            composeTestRule.onNodeWithText(
                context.getString(R.string.caffeine_adjustment_decrease, 1)
            ).performClick()
        }
        composeTestRule.onNodeWithText(
            context.getString(R.string.caffeine_label, 0)
        ).assertIsDisplayed()

        // Tap +1 -> 1
        composeTestRule.onNodeWithText(
            context.getString(R.string.caffeine_adjustment_increase, 1)
        ).performClick()
        composeTestRule.onNodeWithText(
            context.getString(R.string.caffeine_label, 1)
        ).assertIsDisplayed()
    }

    @Test
    fun addDrinkScreen_confirmation_caffeineStepper_adjustsValue() {
        val drinks = listOf(
            DrinkEntity(1, "Espresso", 63, 30, true)
        )
        val fakeViewModel = createFakeViewModel(drinks)
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                AddDrinkScreen(
                    onNavigateBack = {},
                    viewModel = fakeViewModel
                )
            }
        }

        // Tap the drink -> confirmation shown
        composeTestRule.onNodeWithText("Espresso").performClick()

        // Default caffeine value
        composeTestRule.onNodeWithText(
            context.getString(R.string.caffeine_label, 63)
        ).assertIsDisplayed()

        // Tap +5 -> 68
        composeTestRule.onNodeWithText("+5").performClick()
        composeTestRule.onNodeWithText(
            context.getString(R.string.caffeine_label, 68)
        ).assertIsDisplayed()

        // Tap -5 twice -> 58
        composeTestRule.onNodeWithText("-5").performClick()
        composeTestRule.onNodeWithText("-5").performClick()
        composeTestRule.onNodeWithText(
            context.getString(R.string.caffeine_label, 58)
        ).assertIsDisplayed()
    }

    @Test
    fun addDrinkScreen_confirmation_volumeStepper_adjustsValue() {
        val drinks = listOf(
            DrinkEntity(1, "Tea", 30, 200, true)
        )
        val fakeViewModel = createFakeViewModel(drinks)
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                AddDrinkScreen(
                    onNavigateBack = {},
                    viewModel = fakeViewModel
                )
            }
        }

        // Tap the drink -> confirmation shown
        composeTestRule.onNodeWithText("Tea").performClick()

        // Default volume
        composeTestRule.onNodeWithText(
            context.getString(R.string.volume_label, 200)
        ).assertIsDisplayed()

        // Tap +10 -> 210
        composeTestRule.onNodeWithText("+10").performClick()
        composeTestRule.onNodeWithText(
            context.getString(R.string.volume_label, 210)
        ).assertIsDisplayed()

        // Tap -10 twice -> 190
        composeTestRule.onNodeWithText("-10").performClick()
        composeTestRule.onNodeWithText("-10").performClick()
        composeTestRule.onNodeWithText(
            context.getString(R.string.volume_label, 190)
        ).assertIsDisplayed()
    }

    @Test
    fun addDrinkScreen_confirmation_logDrink_callsViewModelWithAdjustedValues() {
        val drinks = listOf(
            DrinkEntity(1, "Espresso", 63, 30, true)
        )
        val fakeViewModel = mockk<DrinkViewModel>(relaxed = true)
        every { fakeViewModel.allDrinks } returns MutableStateFlow(drinks) as StateFlow<List<DrinkEntity>>
        every { fakeViewModel.uiState } returns MutableStateFlow(DrinkUiState()) as StateFlow<DrinkUiState>

        // Make logDrink call onComplete immediately
        every { fakeViewModel.logDrink(any(), any(), any()) } answers {
            val onComplete = thirdArg<() -> Unit>()
            onComplete()
        }

        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                AddDrinkScreen(
                    onNavigateBack = {},
                    viewModel = fakeViewModel
                )
            }
        }

        // Tap the drink
        composeTestRule.onNodeWithText("Espresso").performClick()

        // Adjust caffeine +5 and volume +10
        composeTestRule.onNodeWithText("+5").performClick()
        composeTestRule.onNodeWithText("+10").performClick()

        // Tap Log drink
        composeTestRule.onNodeWithText(context.getString(R.string.log_drink)).performClick()

        // Verify logDrink was called with adjusted values (68 mg, 40 ml)
        val drinkSlot = slot<DrinkEntity>()
        verify { fakeViewModel.logDrink(capture(drinkSlot), any(), any()) }
        assertEquals("Espresso", drinkSlot.captured.name)
        assertEquals(68, drinkSlot.captured.caffeineMg)
        assertEquals(40, drinkSlot.captured.volumeMl)
    }

    @Test
    fun addDrinkScreen_confirmation_logDrink_withoutAdjustment_callsViewModelWithDefaultValues() {
        val drinks = listOf(
            DrinkEntity(1, "Latte", 63, 250, true)
        )
        val fakeViewModel = mockk<DrinkViewModel>(relaxed = true)
        every { fakeViewModel.allDrinks } returns MutableStateFlow(drinks) as StateFlow<List<DrinkEntity>>
        every { fakeViewModel.uiState } returns MutableStateFlow(DrinkUiState()) as StateFlow<DrinkUiState>

        every { fakeViewModel.logDrink(any(), any(), any()) } answers {
            val onComplete = thirdArg<() -> Unit>()
            onComplete()
        }

        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                AddDrinkScreen(
                    onNavigateBack = {},
                    viewModel = fakeViewModel
                )
            }
        }

        // Tap the drink
        composeTestRule.onNodeWithText("Latte").performClick()
        // Tap Log drink immediately (no adjustment)
        composeTestRule.onNodeWithText(context.getString(R.string.log_drink)).performClick()

        // Verify default values were used
        val drinkSlot = slot<DrinkEntity>()
        verify { fakeViewModel.logDrink(capture(drinkSlot), any(), any()) }
        assertEquals("Latte", drinkSlot.captured.name)
        assertEquals(63, drinkSlot.captured.caffeineMg)
        assertEquals(250, drinkSlot.captured.volumeMl)
    }

    @Test
    fun addDrinkScreen_confirmation_logDrink_callsOnError_reEnablesButton() {
        val drinks = listOf(
            DrinkEntity(1, "Espresso", 63, 30, true)
        )
        val fakeViewModel = mockk<DrinkViewModel>(relaxed = true)
        every { fakeViewModel.allDrinks } returns MutableStateFlow(drinks) as StateFlow<List<DrinkEntity>>
        every { fakeViewModel.uiState } returns MutableStateFlow(DrinkUiState()) as StateFlow<DrinkUiState>

        // Make logDrink call onError (simulates failure)
        every { fakeViewModel.logDrink(any(), any(), any()) } answers {
            val onError = arg<() -> Unit>(2)
            onError()
        }

        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                AddDrinkScreen(
                    onNavigateBack = {},
                    viewModel = fakeViewModel
                )
            }
        }

        // Tap the drink -> confirmation shown
        composeTestRule.onNodeWithText("Espresso").performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.adjust_serving)).assertIsDisplayed()

        // Tap Log drink (will fail)
        composeTestRule.onNodeWithText(context.getString(R.string.log_drink)).performClick()

        // Dialog should still be visible (user can retry)
        composeTestRule.onNodeWithText(context.getString(R.string.adjust_serving)).assertIsDisplayed()
        // Cancel button should also still be visible (not stuck in loading)
        composeTestRule.onNodeWithText(context.getString(R.string.cancel)).assertIsDisplayed()
    }

    @Test
    fun addDrinkScreen_displaysCorrectCaffeineValues() {
        val drinks = listOf(
            DrinkEntity(1, "Espresso", 63, 30, true),
            DrinkEntity(2, "Podwójne espresso", 126, 60, true),
            DrinkEntity(3, "Czarna kawa", 95, 250, true)
        )
        val fakeViewModel = createFakeViewModel(drinks)
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                AddDrinkScreen(
                    onNavigateBack = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithText("63 mg").assertIsDisplayed()
        composeTestRule.onNodeWithText("126 mg").assertIsDisplayed()
        composeTestRule.onNodeWithText("95 mg").assertIsDisplayed()
    }

    @Test
    fun addDrinkScreen_showsEmptyMessage_whenNoDrinks() {
        val fakeViewModel = createFakeViewModel(emptyList())
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                AddDrinkScreen(
                    onNavigateBack = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.no_drinks_defined)).assertIsDisplayed()
    }

    private fun createFakeViewModel(drinks: List<DrinkEntity>): DrinkViewModel {
        val vm = mockk<DrinkViewModel>(relaxed = true)
        every { vm.allDrinks } returns MutableStateFlow(drinks) as StateFlow<List<DrinkEntity>>
        every { vm.uiState } returns MutableStateFlow(DrinkUiState()) as StateFlow<DrinkUiState>
        return vm
    }
}
