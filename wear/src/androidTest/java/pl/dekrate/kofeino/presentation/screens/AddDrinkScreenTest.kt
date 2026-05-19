package pl.dekrate.kofeino.presentation.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
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
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AddDrinkScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun addDrinkScreen_displaysTitle() {
        val fakeViewModel = createFakeViewModel(emptyList())
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                AddDrinkScreen(
                    onDrinkAdded = {},
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
            KofeinoTrackerTheme {
                AddDrinkScreen(
                    onDrinkAdded = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithText("Espresso", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Cappuccino", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Czarna kawa", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Energy drink", substring = true).assertIsDisplayed()
    }

    @Test
    fun addDrinkScreen_clickDrink_showsConfirmationDialog() {
        val drinks = listOf(
            DrinkEntity(1, "Espresso", 63, 30, true)
        )
        val fakeViewModel = createFakeViewModel(drinks)
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                AddDrinkScreen(
                    onDrinkAdded = {},
                    viewModel = fakeViewModel
                )
            }
        }

        // Tap the drink
        composeTestRule.onNodeWithText("Espresso", substring = true).performClick()

        // Confirmation dialog should appear with drink name
        composeTestRule.onNodeWithText("Espresso").assertIsDisplayed()
        // Adjust serving label should be visible
        composeTestRule.onNodeWithText(context.getString(R.string.adjust_serving)).assertIsDisplayed()
        // Caffeine and volume labels should be visible
        composeTestRule.onNodeWithText(context.getString(R.string.caffeine_label, 63), substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "${context.getString(R.string.volume)}: 30ml",
            substring = true
        ).assertIsDisplayed()
        // Log drink button should be visible
        composeTestRule.onNodeWithText(context.getString(R.string.log_drink)).assertIsDisplayed()
        // Cancel button should be visible
        composeTestRule.onNodeWithText(context.getString(R.string.cancel)).assertIsDisplayed()

        // Drink list should no longer be shown
        composeTestRule.onNodeWithText(context.getString(R.string.select_drink)).assertIsNotDisplayed()
    }

    @Test
    fun addDrinkScreen_confirmation_cancel_returnsToList() {
        val drinks = listOf(
            DrinkEntity(1, "Espresso", 63, 30, true)
        )
        val fakeViewModel = createFakeViewModel(drinks)
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                AddDrinkScreen(
                    onDrinkAdded = {},
                    viewModel = fakeViewModel
                )
            }
        }

        // Tap the drink -> confirmation shown
        composeTestRule.onNodeWithText("Espresso", substring = true).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.adjust_serving)).assertIsDisplayed()

        // Tap Cancel
        composeTestRule.onNodeWithText(context.getString(R.string.cancel)).performClick()

        // Drink list should be back
        composeTestRule.onNodeWithText(context.getString(R.string.select_drink)).assertIsDisplayed()
        // Confirmation should be gone
        composeTestRule.onNodeWithText(context.getString(R.string.adjust_serving)).assertIsNotDisplayed()
    }

    @Test
    fun addDrinkScreen_confirmation_caffeineFineStepper_adjustsValue() {
        val drinks = listOf(
            DrinkEntity(1, "Espresso", 63, 30, true)
        )
        val fakeViewModel = createFakeViewModel(drinks)
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                AddDrinkScreen(
                    onDrinkAdded = {},
                    viewModel = fakeViewModel
                )
            }
        }

        // Tap the drink -> confirmation shown
        composeTestRule.onNodeWithText("Espresso", substring = true).performClick()

        // Default caffeine value
        composeTestRule.onNodeWithText(
            context.getString(R.string.caffeine_label, 63),
            substring = true
        ).assertIsDisplayed()

        // Tap +1 -> 64
        composeTestRule.onNodeWithText(
            context.getString(R.string.caffeine_adjustment_increase, 1)
        ).performClick()
        composeTestRule.onNodeWithText(
            context.getString(R.string.caffeine_label, 64),
            substring = true
        ).assertIsDisplayed()

        // Tap -1 twice -> 62
        composeTestRule.onNodeWithText(
            context.getString(R.string.caffeine_adjustment_decrease, 1)
        ).performClick()
        composeTestRule.onNodeWithText(
            context.getString(R.string.caffeine_adjustment_decrease, 1)
        ).performClick()
        composeTestRule.onNodeWithText(
            context.getString(R.string.caffeine_label, 62),
            substring = true
        ).assertIsDisplayed()
    }

    @Test
    fun addDrinkScreen_confirmation_caffeineStepper_adjustsValue() {
        val drinks = listOf(
            DrinkEntity(1, "Espresso", 63, 30, true)
        )
        val fakeViewModel = createFakeViewModel(drinks)
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                AddDrinkScreen(
                    onDrinkAdded = {},
                    viewModel = fakeViewModel
                )
            }
        }

        // Tap the drink -> confirmation shown
        composeTestRule.onNodeWithText("Espresso", substring = true).performClick()

        // Default caffeine value displayed
        composeTestRule.onNodeWithText(
            context.getString(R.string.caffeine_label, 63),
            substring = true
        ).assertIsDisplayed()

        // Tap +5
        composeTestRule.onNodeWithText("+5").performClick()
        composeTestRule.onNodeWithText(
            context.getString(R.string.caffeine_label, 68),
            substring = true
        ).assertIsDisplayed()

        // Tap -5 twice (back to 63, then 58)
        composeTestRule.onNodeWithText("-5").performClick()
        composeTestRule.onNodeWithText("-5").performClick()
        composeTestRule.onNodeWithText(
            context.getString(R.string.caffeine_label, 58),
            substring = true
        ).assertIsDisplayed()
    }

    @Test
    fun addDrinkScreen_confirmation_caffeineStepper_minimumGuard() {
        val drinks = listOf(
            DrinkEntity(1, "Espresso", 63, 30, true)
        )
        val fakeViewModel = createFakeViewModel(drinks)
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                AddDrinkScreen(
                    onDrinkAdded = {},
                    viewModel = fakeViewModel
                )
            }
        }

        // Tap the drink -> confirmation shown
        composeTestRule.onNodeWithText("Espresso", substring = true).performClick()

        // Click -5 repeatedly: 63 -> 58 -> 53 -> ... -> 3 (12 clicks, 63-12*5=3)
        repeat(12) { composeTestRule.onNodeWithText("-5").performClick() }

        // Value should be 3 (minimum is 0, but it stops decreasing once < 5)
        composeTestRule.onNodeWithText(
            context.getString(R.string.caffeine_label, 3),
            substring = true
        ).assertIsDisplayed()

        // -5 button should be disabled since 3 < 5
        composeTestRule.onNodeWithText("-5").assertIsNotEnabled()

        // +5 should still work
        composeTestRule.onNodeWithText("+5").performClick()
        composeTestRule.onNodeWithText(
            context.getString(R.string.caffeine_label, 8),
            substring = true
        ).assertIsDisplayed()

        // After going back above 5, -5 should be enabled again
        composeTestRule.onNodeWithText("-5").assertIsEnabled()
        composeTestRule.onNodeWithText("-5").performClick()
        composeTestRule.onNodeWithText(
            context.getString(R.string.caffeine_label, 3),
            substring = true
        ).assertIsDisplayed()
    }

    @Test
    fun addDrinkScreen_confirmation_volumeStepper_adjustsValue() {
        val drinks = listOf(
            DrinkEntity(1, "Tea", 30, 200, true)
        )
        val fakeViewModel = createFakeViewModel(drinks)
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                AddDrinkScreen(
                    onDrinkAdded = {},
                    viewModel = fakeViewModel
                )
            }
        }

        // Tap the drink -> confirmation shown
        composeTestRule.onNodeWithText("Tea", substring = true).performClick()

        // Default volume displayed
        composeTestRule.onNodeWithText(
            "${context.getString(R.string.volume)}: 200ml", substring = true
        ).assertIsDisplayed()

        // Tap +10
        composeTestRule.onNodeWithText("+10").performClick()
        composeTestRule.onNodeWithText(
            "${context.getString(R.string.volume)}: 210ml", substring = true
        ).assertIsDisplayed()

        // Tap -10 twice (back to 200, then 190)
        composeTestRule.onNodeWithText("-10").performClick()
        composeTestRule.onNodeWithText("-10").performClick()
        composeTestRule.onNodeWithText(
            "${context.getString(R.string.volume)}: 190ml", substring = true
        ).assertIsDisplayed()
    }

    @Test
    fun addDrinkScreen_confirmation_volumeStepper_minimumGuard() {
        val drinks = listOf(
            DrinkEntity(1, "Espresso", 63, 30, true)
        )
        val fakeViewModel = createFakeViewModel(drinks)
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                AddDrinkScreen(
                    onDrinkAdded = {},
                    viewModel = fakeViewModel
                )
            }
        }

        // Tap the drink -> confirmation shown
        composeTestRule.onNodeWithText("Espresso", substring = true).performClick()

        // Click -10 three times: 30 -> 20 -> 10 -> 0
        composeTestRule.onNodeWithText("-10").performClick()
        composeTestRule.onNodeWithText("-10").performClick()
        composeTestRule.onNodeWithText("-10").performClick()

        // Value should be 0 (minimum guard stops at 0)
        composeTestRule.onNodeWithText(
            "${context.getString(R.string.volume)}: 0ml", substring = true
        ).assertIsDisplayed()

        // -10 button should be disabled since 0 < 10
        composeTestRule.onNodeWithText("-10").assertIsNotEnabled()

        // +10 should still work
        composeTestRule.onNodeWithText("+10").performClick()
        composeTestRule.onNodeWithText(
            "${context.getString(R.string.volume)}: 10ml", substring = true
        ).assertIsDisplayed()

        // After going back above 10, -10 should be enabled again
        composeTestRule.onNodeWithText("-10").assertIsEnabled()
        composeTestRule.onNodeWithText("-10").performClick()
        composeTestRule.onNodeWithText(
            "${context.getString(R.string.volume)}: 0ml", substring = true
        ).assertIsDisplayed()
    }

    @Test
    fun addDrinkScreen_confirmation_logDrink_callsViewModelAddDrinkWithAdjustedValues() {
        val drinks = listOf(
            DrinkEntity(1, "Espresso", 63, 30, true)
        )
        val fakeViewModel = mockk<CaffeineViewModel>(relaxed = true)
        every { fakeViewModel.uiState } returns MutableStateFlow(
            CaffeineUiState(drinks = drinks)
        ) as StateFlow<CaffeineUiState>

        // Make addDrink call onComplete immediately (simulates successful save)
        every { fakeViewModel.addDrink(any(), any(), any()) } answers {
            val onComplete = thirdArg<() -> Unit>()
            onComplete()
        }

        var navigatedBack = false
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                AddDrinkScreen(
                    onDrinkAdded = { navigatedBack = true },
                    viewModel = fakeViewModel
                )
            }
        }

        // Tap the drink
        composeTestRule.onNodeWithText("Espresso", substring = true).performClick()

        // Adjust caffeine +5 and volume +10
        composeTestRule.onNodeWithText("+5").performClick()
        composeTestRule.onNodeWithText("+10").performClick()

        // Tap Log drink
        composeTestRule.onNodeWithText(context.getString(R.string.log_drink)).performClick()

        // Verify addDrink was called with adjusted values (68 mg, 40 ml)
        val drinkSlot = slot<DrinkEntity>()
        verify { fakeViewModel.addDrink(capture(drinkSlot), any(), any()) }
        assertEquals("Espresso", drinkSlot.captured.name)
        assertEquals(68, drinkSlot.captured.caffeineMg)
        assertEquals(40, drinkSlot.captured.volumeMl)

        // Navigation callback should have fired
        assertTrue("onDrinkAdded should fire after successful log", navigatedBack)
    }

    @Test
    fun addDrinkScreen_confirmation_logDrink_withoutAdjustment_callsViewModelWithDefaultValues() {
        val drinks = listOf(
            DrinkEntity(1, "Latte", 63, 250, true)
        )
        val fakeViewModel = mockk<CaffeineViewModel>(relaxed = true)
        every { fakeViewModel.uiState } returns MutableStateFlow(
            CaffeineUiState(drinks = drinks)
        ) as StateFlow<CaffeineUiState>

        every { fakeViewModel.addDrink(any(), any(), any()) } answers {
            val onComplete = thirdArg<() -> Unit>()
            onComplete()
        }

        var navigatedBack = false
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                AddDrinkScreen(
                    onDrinkAdded = { navigatedBack = true },
                    viewModel = fakeViewModel
                )
            }
        }

        // Tap the drink
        composeTestRule.onNodeWithText("Latte", substring = true).performClick()
        // Tap Log drink immediately (no adjustment)
        composeTestRule.onNodeWithText(context.getString(R.string.log_drink)).performClick()

        // Verify default values were used
        val drinkSlot = slot<DrinkEntity>()
        verify { fakeViewModel.addDrink(capture(drinkSlot), any(), any()) }
        assertEquals("Latte", drinkSlot.captured.name)
        assertEquals(63, drinkSlot.captured.caffeineMg)
        assertEquals(250, drinkSlot.captured.volumeMl)

        assertTrue("onDrinkAdded should fire after successful log", navigatedBack)
    }

    @Test
    fun addDrinkScreen_confirmation_logDrink_callsOnError_reEnablesButton() {
        val drinks = listOf(
            DrinkEntity(1, "Espresso", 63, 30, true)
        )
        val fakeViewModel = mockk<CaffeineViewModel>(relaxed = true)
        every { fakeViewModel.uiState } returns MutableStateFlow(
            CaffeineUiState(drinks = drinks)
        ) as StateFlow<CaffeineUiState>

        // Make addDrink call onError (simulates DB failure)
        every { fakeViewModel.addDrink(any(), any(), any()) } answers {
            val onError = arg<() -> Unit>(2)
            onError()
        }

        composeTestRule.setContent {
            KofeinoTrackerTheme {
                AddDrinkScreen(
                    onDrinkAdded = {},
                    viewModel = fakeViewModel
                )
            }
        }

        // Tap the drink -> confirmation shown
        composeTestRule.onNodeWithText("Espresso", substring = true).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.adjust_serving)).assertIsDisplayed()

        // Tap Log drink (will fail)
        composeTestRule.onNodeWithText(context.getString(R.string.log_drink)).performClick()

        // Dialog should still be visible (user can retry)
        composeTestRule.onNodeWithText(context.getString(R.string.adjust_serving)).assertIsDisplayed()
        // Cancel button should also still be visible (not stuck in loading)
        composeTestRule.onNodeWithText(context.getString(R.string.cancel)).assertIsDisplayed()
        // Log drink button should be enabled for retry
        composeTestRule.onNodeWithText(context.getString(R.string.log_drink)).assertIsEnabled()
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
            KofeinoTrackerTheme {
                AddDrinkScreen(
                    onDrinkAdded = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithText("63 mg", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("126 mg", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("95 mg", substring = true).assertIsDisplayed()
    }

    @Test
    fun addDrinkScreen_showsEmptyMessage_whenNoDrinks() {
        val fakeViewModel = createFakeViewModel(emptyList())
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                AddDrinkScreen(
                    onDrinkAdded = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.no_drinks_defined)).assertIsDisplayed()
    }

    private fun createFakeViewModel(drinks: List<DrinkEntity>): CaffeineViewModel {
        val vm = mockk<CaffeineViewModel>(relaxed = true)
        every { vm.uiState } returns MutableStateFlow(
            CaffeineUiState(drinks = drinks)
        ) as StateFlow<CaffeineUiState>
        return vm
    }
}
