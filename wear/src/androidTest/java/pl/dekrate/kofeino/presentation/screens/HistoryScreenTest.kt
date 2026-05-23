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
import pl.dekrate.kofeino.common.domain.model.CaffeineIntake
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

class HistoryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun historyScreen_displaysTitleAndTotal() {
        val fakeViewModel = createFakeViewModel(CaffeineUiState(totalCaffeineMg = 200))
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                HistoryScreen(
                    onEditIntake = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.history)).assertIsDisplayed()
        composeTestRule.onNodeWithText("200 mg", substring = true).assertIsDisplayed()
    }

    @Test
    fun historyScreen_displaysIntakeItems() {
        val intakes = listOf(
            CaffeineIntake(1, drinkName = "Latte", caffeineMg = 63, volumeMl = 250, timestamp = 0L),
            CaffeineIntake(2, drinkName = "Zielona herbata", caffeineMg = 28, volumeMl = 250, timestamp = 0L)
        )
        val fakeViewModel = createFakeViewModel(
            CaffeineUiState(dateIntakes = intakes, totalCaffeineMg = 91)
        )
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                HistoryScreen(
                    onEditIntake = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithText("Latte", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Zielona herbata", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("63 mg", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("28 mg", substring = true).assertIsDisplayed()
    }

    @Test
    fun historyScreen_showsEmptyMessage_whenNoIntakes() {
        val fakeViewModel = createFakeViewModel(CaffeineUiState(dateIntakes = emptyList()))
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                HistoryScreen(
                    onEditIntake = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.no_drinks_today)).assertIsDisplayed()
    }

    @Test
    fun historyScreen_displaysMultipleItems() {
        val intakes = List(5) { index ->
            CaffeineIntake(index.toLong(), drinkName = "drink_$index", caffeineMg = 50, volumeMl = 200, timestamp = 0L)
        }
        val fakeViewModel = createFakeViewModel(CaffeineUiState(dateIntakes = intakes))
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                HistoryScreen(
                    onEditIntake = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onAllNodesWithText("mg", substring = true).assertCountEquals(5)
    }

    @Test
    fun historyScreen_showsDateLabel() {
        val fakeViewModel = createFakeViewModel(CaffeineUiState(dateLabel = context.getString(R.string.today)))
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                HistoryScreen(
                    onEditIntake = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.today)).assertIsDisplayed()
    }

    @Test
    fun historyScreen_showsDateLabel_forYesterday() {
        val fakeViewModel = createFakeViewModel(CaffeineUiState(dateLabel = context.getString(R.string.yesterday)))
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                HistoryScreen(
                    onEditIntake = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.yesterday)).assertIsDisplayed()
    }

    @Test
    fun historyScreen_navigatesPreviousDay() {
        val fakeViewModel = mockk<CaffeineViewModel>(relaxed = true)
        every { fakeViewModel.uiState } returns MutableStateFlow(
            CaffeineUiState(dateLabel = context.getString(R.string.today))
        ) as StateFlow<CaffeineUiState>

        composeTestRule.setContent {
            KofeinoTrackerTheme {
                HistoryScreen(
                    onEditIntake = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithText("◀").performClick()
        verify { fakeViewModel.previousDay() }
    }

    @Test
    fun historyScreen_navigatesNextDay() {
        val fakeViewModel = mockk<CaffeineViewModel>(relaxed = true)
        every { fakeViewModel.uiState } returns MutableStateFlow(
            CaffeineUiState(dateLabel = context.getString(R.string.today))
        ) as StateFlow<CaffeineUiState>

        composeTestRule.setContent {
            KofeinoTrackerTheme {
                HistoryScreen(
                    onEditIntake = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithText("▶").performClick()
        verify { fakeViewModel.nextDay() }
    }

    @Test
    fun historyScreen_showsTodayButton_whenNotToday() {
        val fakeViewModel = mockk<CaffeineViewModel>(relaxed = true)
        every { fakeViewModel.uiState } returns MutableStateFlow(
            CaffeineUiState(dateLabel = context.getString(R.string.yesterday))
        ) as StateFlow<CaffeineUiState>
        every { fakeViewModel.isToday() } returns false

        composeTestRule.setContent {
            KofeinoTrackerTheme {
                HistoryScreen(
                    onEditIntake = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.today)).assertIsDisplayed()
    }

    @Test
    fun historyScreen_intakeClick_callsOnEditIntake() {
        var editedId = -1L
        val intakes = listOf(
            CaffeineIntake(99, drinkName = "Espresso", caffeineMg = 63, volumeMl = 30, timestamp = 0L)
        )
        val fakeViewModel = createFakeViewModel(CaffeineUiState(dateIntakes = intakes))
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                HistoryScreen(
                    onEditIntake = { editedId = it },
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithText("Espresso", substring = true).performClick()
        assert(editedId == 99L)
    }

    // ===== Accessibility content description tests =====

    @Test
    fun historyScreen_previousDayButton_hasContentDescription() {
        val fakeViewModel = mockk<CaffeineViewModel>(relaxed = true)
        every { fakeViewModel.uiState } returns MutableStateFlow(
            CaffeineUiState(dateLabel = context.getString(R.string.today))
        ) as StateFlow<CaffeineUiState>

        composeTestRule.setContent {
            KofeinoTrackerTheme {
                HistoryScreen(
                    onEditIntake = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.previous_day))
            .assertIsDisplayed()
    }

    @Test
    fun historyScreen_nextDayButton_hasContentDescription() {
        val fakeViewModel = mockk<CaffeineViewModel>(relaxed = true)
        every { fakeViewModel.uiState } returns MutableStateFlow(
            CaffeineUiState(dateLabel = context.getString(R.string.today))
        ) as StateFlow<CaffeineUiState>

        composeTestRule.setContent {
            KofeinoTrackerTheme {
                HistoryScreen(
                    onEditIntake = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.next_day))
            .assertIsDisplayed()
    }

    private fun createFakeViewModel(state: CaffeineUiState): CaffeineViewModel {
        val vm = mockk<CaffeineViewModel>(relaxed = true)
        every { vm.uiState } returns MutableStateFlow(state) as StateFlow<CaffeineUiState>
        return vm
    }
}
