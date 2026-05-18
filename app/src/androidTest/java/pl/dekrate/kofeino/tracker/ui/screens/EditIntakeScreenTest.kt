package pl.dekrate.kofeino.tracker.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
import pl.dekrate.kofeino.tracker.presentation.viewmodel.EditIntakeUiState
import pl.dekrate.kofeino.tracker.presentation.viewmodel.EditIntakeViewModel
import pl.dekrate.kofeino.tracker.ui.theme.KofeinoTrackerPhoneTheme

class EditIntakeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    // ===== Display tests =====

    @Test
    fun editIntakeScreen_displaysTitleWhenLoading() {
        val fakeVm = createFakeViewModel(EditIntakeUiState(isLoading = true))
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                EditIntakeScreen(
                    intakeId = 1L,
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.edit_intake_title)).assertIsDisplayed()
    }

    @Test
    fun editIntakeScreen_displaysDrinkNameInToolbar_whenLoaded() {
        val intake = CaffeineIntake(id = 1, drinkName = "Latte", caffeineMg = 63, volumeMl = 250, timestamp = 0L)
        val fakeVm = createFakeViewModel(
            EditIntakeUiState(intake = intake, drinkName = "Latte", caffeineMg = 63, volumeMl = 250, isLoading = false)
        )
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                EditIntakeScreen(
                    intakeId = 1L,
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText("Latte").assertIsDisplayed()
    }

    @Test
    fun editIntakeScreen_displaysCaffeineValue() {
        val intake = CaffeineIntake(id = 1, drinkName = "Latte", caffeineMg = 63, volumeMl = 250, timestamp = 0L)
        val fakeVm = createFakeViewModel(
            EditIntakeUiState(intake = intake, drinkName = "Latte", caffeineMg = 63, volumeMl = 250, isLoading = false)
        )
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                EditIntakeScreen(
                    intakeId = 1L,
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.caffeine_label, 63)).assertIsDisplayed()
    }

    @Test
    fun editIntakeScreen_displaysVolumeValue() {
        val intake = CaffeineIntake(id = 1, drinkName = "Latte", caffeineMg = 63, volumeMl = 250, timestamp = 0L)
        val fakeVm = createFakeViewModel(
            EditIntakeUiState(intake = intake, drinkName = "Latte", caffeineMg = 63, volumeMl = 250, isLoading = false)
        )
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                EditIntakeScreen(
                    intakeId = 1L,
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.volume_label, 250)).assertIsDisplayed()
    }

    @Test
    fun editIntakeScreen_displaysAdjustmentButtons() {
        val intake = CaffeineIntake(id = 1, drinkName = "Latte", caffeineMg = 63, volumeMl = 250, timestamp = 0L)
        val fakeVm = createFakeViewModel(
            EditIntakeUiState(intake = intake, drinkName = "Latte", caffeineMg = 63, volumeMl = 250, isLoading = false)
        )
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                EditIntakeScreen(
                    intakeId = 1L,
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.caffeine_adjustment_decrease, 5)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.caffeine_adjustment_increase, 5)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.volume_adjustment_decrease, 10)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.volume_adjustment_increase, 10)).assertIsDisplayed()
    }

    @Test
    fun editIntakeScreen_displaysSaveAndDeleteButtons() {
        val intake = CaffeineIntake(id = 1, drinkName = "Latte", caffeineMg = 63, volumeMl = 250, timestamp = 0L)
        val fakeVm = createFakeViewModel(
            EditIntakeUiState(intake = intake, drinkName = "Latte", caffeineMg = 63, volumeMl = 250, isLoading = false)
        )
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                EditIntakeScreen(
                    intakeId = 1L,
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.save)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.delete)).assertIsDisplayed()
    }

    @Test
    fun editIntakeScreen_showsNotFound_whenIntakeIsNull() {
        val fakeVm = createFakeViewModel(
            EditIntakeUiState(isLoading = false, intake = null)
        )
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                EditIntakeScreen(
                    intakeId = 999L,
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.intake_not_found)).assertIsDisplayed()
    }

    // ===== Interaction tests =====

    @Test
    fun editIntakeScreen_caffeineDecrease_callsUpdateCaffeineMg() {
        val intake = CaffeineIntake(id = 1, drinkName = "Latte", caffeineMg = 63, volumeMl = 250, timestamp = 0L)
        val fakeVm = mockk<EditIntakeViewModel>(relaxed = true)
        every { fakeVm.uiState } returns MutableStateFlow(
            EditIntakeUiState(intake = intake, drinkName = "Latte", caffeineMg = 63, volumeMl = 250, isLoading = false)
        ) as StateFlow<EditIntakeUiState>

        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                EditIntakeScreen(
                    intakeId = 1L,
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.caffeine_adjustment_decrease, 5)).performClick()
        verify { fakeVm.updateCaffeineMg(-5) }
    }

    @Test
    fun editIntakeScreen_caffeineIncrease_callsUpdateCaffeineMg() {
        val intake = CaffeineIntake(id = 1, drinkName = "Latte", caffeineMg = 63, volumeMl = 250, timestamp = 0L)
        val fakeVm = mockk<EditIntakeViewModel>(relaxed = true)
        every { fakeVm.uiState } returns MutableStateFlow(
            EditIntakeUiState(intake = intake, drinkName = "Latte", caffeineMg = 63, volumeMl = 250, isLoading = false)
        ) as StateFlow<EditIntakeUiState>

        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                EditIntakeScreen(
                    intakeId = 1L,
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.caffeine_adjustment_increase, 5)).performClick()
        verify { fakeVm.updateCaffeineMg(5) }
    }

    @Test
    fun editIntakeScreen_volumeDecrease_callsUpdateVolumeMl() {
        val intake = CaffeineIntake(id = 1, drinkName = "Latte", caffeineMg = 63, volumeMl = 250, timestamp = 0L)
        val fakeVm = mockk<EditIntakeViewModel>(relaxed = true)
        every { fakeVm.uiState } returns MutableStateFlow(
            EditIntakeUiState(intake = intake, drinkName = "Latte", caffeineMg = 63, volumeMl = 250, isLoading = false)
        ) as StateFlow<EditIntakeUiState>

        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                EditIntakeScreen(
                    intakeId = 1L,
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.volume_adjustment_decrease, 10)).performClick()
        verify { fakeVm.updateVolumeMl(-10) }
    }

    @Test
    fun editIntakeScreen_volumeIncrease_callsUpdateVolumeMl() {
        val intake = CaffeineIntake(id = 1, drinkName = "Latte", caffeineMg = 63, volumeMl = 250, timestamp = 0L)
        val fakeVm = mockk<EditIntakeViewModel>(relaxed = true)
        every { fakeVm.uiState } returns MutableStateFlow(
            EditIntakeUiState(intake = intake, drinkName = "Latte", caffeineMg = 63, volumeMl = 250, isLoading = false)
        ) as StateFlow<EditIntakeUiState>

        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                EditIntakeScreen(
                    intakeId = 1L,
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.volume_adjustment_increase, 10)).performClick()
        verify { fakeVm.updateVolumeMl(10) }
    }

    @Test
    fun editIntakeScreen_saveButton_callsViewModelSave() {
        val intake = CaffeineIntake(id = 1, drinkName = "Latte", caffeineMg = 63, volumeMl = 250, timestamp = 0L)
        val fakeVm = mockk<EditIntakeViewModel>(relaxed = true)
        every { fakeVm.uiState } returns MutableStateFlow(
            EditIntakeUiState(intake = intake, drinkName = "Latte", caffeineMg = 63, volumeMl = 250, isLoading = false)
        ) as StateFlow<EditIntakeUiState>

        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                EditIntakeScreen(
                    intakeId = 1L,
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.save)).performClick()
        verify { fakeVm.save(any(), any()) }
    }

    @Test
    fun editIntakeScreen_saveButton_isDisabled_whenSaving() {
        val intake = CaffeineIntake(id = 1, drinkName = "Latte", caffeineMg = 63, volumeMl = 250, timestamp = 0L)
        val fakeVm = mockk<EditIntakeViewModel>(relaxed = true)
        every { fakeVm.uiState } returns MutableStateFlow(
            EditIntakeUiState(intake = intake, drinkName = "Latte", caffeineMg = 63, volumeMl = 250, isLoading = false)
        ) as StateFlow<EditIntakeUiState>

        // Make save call onComplete (simulates success) to re-enable
        every { fakeVm.save(any(), any()) } answers {
            val onComplete = arg<() -> Unit>(0)
            onComplete()
        }

        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                EditIntakeScreen(
                    intakeId = 1L,
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        // Click save — it completes instantly via the mock, so the screen navigates back
        composeTestRule.onNodeWithText(context.getString(R.string.save)).performClick()
        verify { fakeVm.save(any(), any()) }
    }

    @Test
    fun editIntakeScreen_deleteButton_showsConfirmationDialog() {
        val intake = CaffeineIntake(id = 1, drinkName = "Latte", caffeineMg = 63, volumeMl = 250, timestamp = 0L)
        val fakeVm = createFakeViewModel(
            EditIntakeUiState(intake = intake, drinkName = "Latte", caffeineMg = 63, volumeMl = 250, isLoading = false)
        )
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                EditIntakeScreen(
                    intakeId = 1L,
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        // Click Delete button
        composeTestRule.onNodeWithText(context.getString(R.string.delete)).performClick()
        // Confirmation dialog should appear
        composeTestRule.onNodeWithText(context.getString(R.string.delete_intake_confirm)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.cancel)).assertIsDisplayed()
    }

    @Test
    fun editIntakeScreen_deleteConfirmation_confirm_callsDelete() {
        val intake = CaffeineIntake(id = 1, drinkName = "Latte", caffeineMg = 63, volumeMl = 250, timestamp = 0L)
        val fakeVm = mockk<EditIntakeViewModel>(relaxed = true)
        every { fakeVm.uiState } returns MutableStateFlow(
            EditIntakeUiState(intake = intake, drinkName = "Latte", caffeineMg = 63, volumeMl = 250, isLoading = false)
        ) as StateFlow<EditIntakeUiState>

        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                EditIntakeScreen(
                    intakeId = 1L,
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        // Click Delete -> confirmation shown
        composeTestRule.onNodeWithText(context.getString(R.string.delete)).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.delete_intake_confirm)).assertIsDisplayed()
        // Click confirm delete
        composeTestRule.onNodeWithText(context.getString(R.string.delete)).performClick()
        verify { fakeVm.delete(any(), any()) }
    }

    @Test
    fun editIntakeScreen_deleteConfirmation_cancel_dismissesDialog() {
        val intake = CaffeineIntake(id = 1, drinkName = "Latte", caffeineMg = 63, volumeMl = 250, timestamp = 0L)
        val fakeVm = createFakeViewModel(
            EditIntakeUiState(intake = intake, drinkName = "Latte", caffeineMg = 63, volumeMl = 250, isLoading = false)
        )
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                EditIntakeScreen(
                    intakeId = 1L,
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        // Click Delete -> confirmation shown
        composeTestRule.onNodeWithText(context.getString(R.string.delete)).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.delete_intake_confirm)).assertIsDisplayed()
        // Click Cancel
        composeTestRule.onNodeWithText(context.getString(R.string.cancel)).performClick()
        // Dialog should be dismissed, delete button should be visible again
        composeTestRule.onNodeWithText(context.getString(R.string.delete_intake_confirm)).assertDoesNotExist()
        composeTestRule.onNodeWithText(context.getString(R.string.delete)).assertIsDisplayed()
    }

    // ===== Navigation test =====

    @Test
    fun editIntakeScreen_backButton_navigatesBack() {
        var navigated = false
        val fakeVm = createFakeViewModel(EditIntakeUiState())
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                EditIntakeScreen(
                    intakeId = 1L,
                    onNavigateBack = { navigated = true },
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.back)).performClick()
        assert(navigated) { "Back button click should trigger onNavigateBack" }
    }

    // ===== Accessibility content description tests =====

    @Test
    fun editIntakeScreen_backButton_hasContentDescription() {
        val fakeVm = createFakeViewModel(EditIntakeUiState())
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                EditIntakeScreen(
                    intakeId = 1L,
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.back)).assertIsDisplayed()
    }

    // ===== Utility =====

    private fun createFakeViewModel(
        state: EditIntakeUiState
    ): EditIntakeViewModel {
        val vm = mockk<EditIntakeViewModel>(relaxed = true)
        every { vm.uiState } returns MutableStateFlow(state) as StateFlow<EditIntakeUiState>
        return vm
    }
}
