package pl.dekrate.kofeino.presentation.screens

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
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
    fun addDrinkScreen_clickDrink_callsViewModelAddDrinkAndNavigatesBack() {
        val drinks = listOf(
            DrinkEntity(1, "Espresso", 63, 30, true)
        )
        val fakeViewModel = mockk<CaffeineViewModel>(relaxed = true)
        every { fakeViewModel.uiState } returns MutableStateFlow(
            CaffeineUiState(drinks = drinks)
        ) as StateFlow<CaffeineUiState>

        var navigatedBack = false
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                AddDrinkScreen(
                    onDrinkAdded = { navigatedBack = true },
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithText("Espresso", substring = true).performClick()

        val drinkSlot = slot<DrinkEntity>()
        verify { fakeViewModel.addDrink(capture(drinkSlot)) }
        assertEquals("Espresso", drinkSlot.captured.name)
        assert(navigatedBack)
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
