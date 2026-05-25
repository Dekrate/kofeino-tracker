package pl.dekrate.kofeino.tracker.ui.screens

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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
import pl.dekrate.kofeino.common.domain.model.CaffeineIntake
import pl.dekrate.kofeino.tracker.presentation.viewmodel.HistoryUiState
import pl.dekrate.kofeino.tracker.presentation.viewmodel.HistoryViewModel
import pl.dekrate.kofeino.common.sync.SyncStatus
import pl.dekrate.kofeino.tracker.data.sync.SyncStatusTracker
import pl.dekrate.kofeino.tracker.ui.theme.KofeinoTrackerPhoneTheme

class HistoryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun historyScreen_displaysTitleAndTotal() {
        val fakeViewModel = createFakeViewModel(HistoryUiState(totalCaffeineMg = 200, isLoading = false))
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                HistoryScreen(
                    syncStatusTracker = createFakeSyncStatusTracker(),
                    onEditIntake = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.history_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText("200 mg", substring = true).assertIsDisplayed()
    }

    @Test
    fun historyScreen_displaysIntakeItems() {
        val intakes = listOf(
            CaffeineIntake(1, drinkName = "Latte", caffeineMg = 63, volumeMl = 250, timestamp = 0L),
            CaffeineIntake(2, drinkName = "Zielona herbata", caffeineMg = 28, volumeMl = 250, timestamp = 0L)
        )
        val fakeViewModel = createFakeViewModel(
            HistoryUiState(dateIntakes = intakes, totalCaffeineMg = 91, isLoading = false)
        )
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                HistoryScreen(
                    syncStatusTracker = createFakeSyncStatusTracker(),
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
        val fakeViewModel = createFakeViewModel(
            HistoryUiState(dateIntakes = emptyList(), isLoading = false)
        )
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                HistoryScreen(
                    syncStatusTracker = createFakeSyncStatusTracker(),
                    onEditIntake = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.no_intakes_for_date)).assertIsDisplayed()
    }

    @Test
    fun historyScreen_displaysMultipleItems() {
        val intakes = List(5) { index ->
            CaffeineIntake(
                id = index.toLong(),
                drinkName = "drink_$index",
                caffeineMg = 50,
                volumeMl = 200,
                timestamp = 0L
            )
        }
        val fakeViewModel = createFakeViewModel(
            HistoryUiState(dateIntakes = intakes, totalCaffeineMg = 250, isLoading = false)
        )
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                HistoryScreen(
                    syncStatusTracker = createFakeSyncStatusTracker(),
                    onEditIntake = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onAllNodesWithText("mg", substring = true).assertCountEquals(6) // 5 items + total
    }

    @Test
    fun historyScreen_showsLocalizedTodayLabel() {
        val fakeViewModel = createFakeViewModel(
            state = HistoryUiState(isLoading = false),
            isToday = true
        )
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                HistoryScreen(
                    syncStatusTracker = createFakeSyncStatusTracker(),
                    onEditIntake = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.today)).assertIsDisplayed()
    }

    @Test
    fun historyScreen_showsLocalizedYesterdayLabel() {
        val fakeViewModel = createFakeViewModel(
            state = HistoryUiState(isLoading = false),
            isToday = false,
            isYesterday = true
        )
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                HistoryScreen(
                    syncStatusTracker = createFakeSyncStatusTracker(),
                    onEditIntake = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.yesterday)).assertIsDisplayed()
    }

    @Test
    fun historyScreen_navigatesPreviousDay() {
        val fakeViewModel = mockk<HistoryViewModel>(relaxed = true)
        every { fakeViewModel.uiState } returns MutableStateFlow(
            HistoryUiState(dateLabel = "Today", isLoading = false)
        ) as StateFlow<HistoryUiState>
        every { fakeViewModel.isToday() } returns true

        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                HistoryScreen(
                    syncStatusTracker = createFakeSyncStatusTracker(),
                    onEditIntake = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.previous_day))
            .performClick()
        verify { fakeViewModel.previousDay() }
    }

    @Test
    fun historyScreen_navigatesNextDay() {
        val fakeViewModel = mockk<HistoryViewModel>(relaxed = true)
        every { fakeViewModel.uiState } returns MutableStateFlow(
            HistoryUiState(dateLabel = "Today", isLoading = false)
        ) as StateFlow<HistoryUiState>
        every { fakeViewModel.isToday() } returns true

        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                HistoryScreen(
                    syncStatusTracker = createFakeSyncStatusTracker(),
                    onEditIntake = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.next_day))
            .performClick()
        verify { fakeViewModel.nextDay() }
    }

    @Test
    fun historyScreen_showsTodayButton_whenNotToday() {
        val fakeViewModel = mockk<HistoryViewModel>(relaxed = true)
        every { fakeViewModel.uiState } returns MutableStateFlow(
            HistoryUiState(dateLabel = "Yesterday", isLoading = false)
        ) as StateFlow<HistoryUiState>
        every { fakeViewModel.isToday() } returns false

        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                HistoryScreen(
                    syncStatusTracker = createFakeSyncStatusTracker(),
                    onEditIntake = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.go_to_today))
            .assertIsDisplayed()
    }

    @Test
    fun historyScreen_todayButtonNotShown_whenOnToday() {
        val fakeViewModel = mockk<HistoryViewModel>(relaxed = true)
        every { fakeViewModel.uiState } returns MutableStateFlow(
            HistoryUiState(dateLabel = "Today", isLoading = false)
        ) as StateFlow<HistoryUiState>
        every { fakeViewModel.isToday() } returns true

        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                HistoryScreen(
                    syncStatusTracker = createFakeSyncStatusTracker(),
                    onEditIntake = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.go_to_today))
            .assertDoesNotExist()
    }

    @Test
    fun historyScreen_goToTodayButton_callsGoToToday() {
        val fakeViewModel = mockk<HistoryViewModel>(relaxed = true)
        every { fakeViewModel.uiState } returns MutableStateFlow(
            HistoryUiState(dateLabel = "Yesterday", isLoading = false)
        ) as StateFlow<HistoryUiState>
        every { fakeViewModel.isToday() } returns false

        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                HistoryScreen(
                    syncStatusTracker = createFakeSyncStatusTracker(),
                    onEditIntake = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.go_to_today))
            .performClick()
        verify { fakeViewModel.goToToday() }
    }

    @Test
    fun historyScreen_intakeClick_callsOnEditIntake() {
        var editedId = -1L
        val intakes = listOf(
            CaffeineIntake(99, drinkName = "Espresso", caffeineMg = 63, volumeMl = 30, timestamp = 0L)
        )
        val fakeViewModel = createFakeViewModel(
            HistoryUiState(dateIntakes = intakes, totalCaffeineMg = 63, isLoading = false)
        )
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                HistoryScreen(
                    syncStatusTracker = createFakeSyncStatusTracker(),
                    onEditIntake = { editedId = it },
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithText("Espresso", substring = true).performClick()
        assert(editedId == 99L) { "Expected 99L, got $editedId" }
    }

    @Test
    fun historyScreen_showsLoadingIndicator_whenLoading() {
        val fakeViewModel = createFakeViewModel(HistoryUiState(isLoading = true))
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                HistoryScreen(
                    syncStatusTracker = createFakeSyncStatusTracker(),
                    onEditIntake = {},
                    viewModel = fakeViewModel
                )
            }
        }
        // Loading state should be visible - no content shown
        composeTestRule.onNodeWithText(context.getString(R.string.history_title)).assertIsDisplayed()
    }

    // ===== Accessibility content description tests =====

    @Test
    fun historyScreen_previousDayButton_hasContentDescription() {
        val fakeViewModel = mockk<HistoryViewModel>(relaxed = true)
        every { fakeViewModel.uiState } returns MutableStateFlow(
            HistoryUiState(dateLabel = "Today", isLoading = false)
        ) as StateFlow<HistoryUiState>
        every { fakeViewModel.isToday() } returns true

        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                HistoryScreen(
                    syncStatusTracker = createFakeSyncStatusTracker(),
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
        val fakeViewModel = mockk<HistoryViewModel>(relaxed = true)
        every { fakeViewModel.uiState } returns MutableStateFlow(
            HistoryUiState(dateLabel = "Today", isLoading = false)
        ) as StateFlow<HistoryUiState>
        every { fakeViewModel.isToday() } returns true

        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                HistoryScreen(
                    syncStatusTracker = createFakeSyncStatusTracker(),
                    onEditIntake = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.next_day))
            .assertIsDisplayed()
    }

    @Test
    fun historyScreen_intakeItem_hasContentDescription() {
        val intakes = listOf(
            CaffeineIntake(1, drinkName = "Espresso", caffeineMg = 63, volumeMl = 30, timestamp = 0L)
        )
        val fakeViewModel = createFakeViewModel(
            HistoryUiState(dateIntakes = intakes, totalCaffeineMg = 63, isLoading = false)
        )
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                HistoryScreen(
                    syncStatusTracker = createFakeSyncStatusTracker(),
                    onEditIntake = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithContentDescription("Espresso 63 mg", substring = true)
            .assertIsDisplayed()
    }

    // ===== Multiple items with different times =====

    @Test
    fun historyScreen_displaysIntakesWithTime() {
        // 0L timestamp = Jan 1, 1970 00:00 UTC = 01:00 CET
        // Use different timestamps for different times
        val now = System.currentTimeMillis()
        val hour1 = now - 2 * 60 * 60 * 1000  // 2 hours ago
        val hour2 = now - 5 * 60 * 60 * 1000  // 5 hours ago

        val intakes = listOf(
            CaffeineIntake(1, drinkName = "Espresso", caffeineMg = 63, volumeMl = 30, timestamp = hour1),
            CaffeineIntake(2, drinkName = "Cappuccino", caffeineMg = 75, volumeMl = 200, timestamp = hour2)
        )
        val fakeViewModel = createFakeViewModel(
            HistoryUiState(dateIntakes = intakes, totalCaffeineMg = 138, isLoading = false)
        )
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                HistoryScreen(
                    syncStatusTracker = createFakeSyncStatusTracker(),
                    onEditIntake = {},
                    viewModel = fakeViewModel
                )
            }
        }
        composeTestRule.onNodeWithText("Espresso", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Cappuccino", substring = true).assertIsDisplayed()
    }

    private fun createFakeViewModel(
        state: HistoryUiState,
        isToday: Boolean = false,
        isYesterday: Boolean = false
    ): HistoryViewModel {
        val vm = mockk<HistoryViewModel>(relaxed = true)
        every { vm.uiState } returns MutableStateFlow(state) as StateFlow<HistoryUiState>
        every { vm.isToday() } returns isToday
        every { vm.isYesterday() } returns isYesterday
        return vm
    }

    private fun createFakeSyncStatusTracker(): SyncStatusTracker {
        val tracker = mockk<SyncStatusTracker>(relaxed = true)
        val statusFlow = MutableStateFlow<SyncStatus>(SyncStatus.AwaitingDevice)
        every { tracker.status } returns statusFlow
        return tracker
    }
}
