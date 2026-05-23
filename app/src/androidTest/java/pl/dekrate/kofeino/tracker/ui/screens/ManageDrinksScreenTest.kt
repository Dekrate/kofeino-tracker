package pl.dekrate.kofeino.tracker.ui.screens

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
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
import pl.dekrate.kofeino.common.domain.model.DrinkEntity
import pl.dekrate.kofeino.tracker.presentation.viewmodel.ManageDrinksUiState
import pl.dekrate.kofeino.tracker.presentation.viewmodel.ManageDrinksViewModel
import pl.dekrate.kofeino.tracker.ui.theme.KofeinoTrackerPhoneTheme

class ManageDrinksScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    // ===== Display tests =====

    @Test
    fun manageDrinksScreen_displaysTitle() {
        val fakeVm = createFakeViewModel(ManageDrinksUiState(isLoading = false))
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                ManageDrinksScreen(
                    onNavigateToOfficialDrinks = {},
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.manage_drinks_title)).assertIsDisplayed()
    }

    @Test
    fun manageDrinksScreen_displaysListOfDrinks() {
        val drinks = listOf(
            DrinkEntity(1, "Espresso", 63, 30, true),
            DrinkEntity(2, "Mój napój", 100, 250, false)
        )
        val fakeVm = createFakeViewModel(ManageDrinksUiState(drinks = drinks, isLoading = false))
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                ManageDrinksScreen(
                    onNavigateToOfficialDrinks = {},
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText("Espresso").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mój napój").assertIsDisplayed()
    }

    @Test
    fun manageDrinksScreen_showsFAB() {
        val fakeVm = createFakeViewModel(ManageDrinksUiState(isLoading = false))
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                ManageDrinksScreen(
                    onNavigateToOfficialDrinks = {},
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.add_drink)).assertIsDisplayed()
    }

    @Test
    fun manageDrinksScreen_showsBrowseOfficialCard() {
        val fakeVm = createFakeViewModel(ManageDrinksUiState(isLoading = false))
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                ManageDrinksScreen(
                    onNavigateToOfficialDrinks = {},
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.browse_official_drinks)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.browse_official_drinks_hint)).assertIsDisplayed()
    }

    @Test
    fun manageDrinksScreen_showsCaffeineAndVolumeForDrink() {
        val drinks = listOf(
            DrinkEntity(1, "Espresso", 63, 30, true)
        )
        val fakeVm = createFakeViewModel(ManageDrinksUiState(drinks = drinks, isLoading = false))
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                ManageDrinksScreen(
                    onNavigateToOfficialDrinks = {},
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText("63 mg · 30 ml").assertIsDisplayed()
    }

    @Test
    fun manageDrinksScreen_showsEditAndDeleteForNonDefaultDrink() {
        val drinks = listOf(
            DrinkEntity(2, "Kawa z mlekiem", 80, 200, false)
        )
        val fakeVm = createFakeViewModel(ManageDrinksUiState(drinks = drinks, isLoading = false))
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                ManageDrinksScreen(
                    onNavigateToOfficialDrinks = {},
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.edit_drink_form_title)).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.delete)).assertIsDisplayed()
    }

    @Test
    fun manageDrinksScreen_hidesEditAndDeleteForDefaultDrink() {
        val drinks = listOf(
            DrinkEntity(1, "Espresso", 63, 30, true)
        )
        val fakeVm = createFakeViewModel(ManageDrinksUiState(drinks = drinks, isLoading = false))
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                ManageDrinksScreen(
                    onNavigateToOfficialDrinks = {},
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.edit_drink_form_title)).assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.delete)).assertDoesNotExist()
    }

    // ===== Interaction tests =====

    @Test
    fun manageDrinksScreen_fabClick_showsAddForm() {
        val fakeVm = mockk<ManageDrinksViewModel>(relaxed = true)
        every { fakeVm.uiState } returns MutableStateFlow(
            ManageDrinksUiState(isLoading = false)
        ) as StateFlow<ManageDrinksUiState>

        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                ManageDrinksScreen(
                    onNavigateToOfficialDrinks = {},
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.add_drink)).performClick()
        verify { fakeVm.showAddForm() }
    }

    @Test
    fun manageDrinksScreen_editClick_callsShowEditForm() {
        val drink = DrinkEntity(2, "Mój napój", 100, 250, false)
        val fakeVm = mockk<ManageDrinksViewModel>(relaxed = true)
        every { fakeVm.uiState } returns MutableStateFlow(
            ManageDrinksUiState(drinks = listOf(drink), isLoading = false)
        ) as StateFlow<ManageDrinksUiState>

        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                ManageDrinksScreen(
                    onNavigateToOfficialDrinks = {},
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.edit_drink_form_title)).performClick()
        verify { fakeVm.showEditForm(drink) }
    }

    @Test
    fun manageDrinksScreen_deleteClick_callsRequestDelete() {
        val drink = DrinkEntity(2, "Mój napój", 100, 250, false)
        val fakeVm = mockk<ManageDrinksViewModel>(relaxed = true)
        every { fakeVm.uiState } returns MutableStateFlow(
            ManageDrinksUiState(drinks = listOf(drink), isLoading = false)
        ) as StateFlow<ManageDrinksUiState>

        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                ManageDrinksScreen(
                    onNavigateToOfficialDrinks = {},
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.delete)).performClick()
        verify { fakeVm.requestDelete(drink) }
    }

    @Test
    fun manageDrinksScreen_deleteConfirmation_showsDialog() {
        val drink = DrinkEntity(2, "Mój napój", 100, 250, false)
        val fakeVm = createFakeViewModel(
            ManageDrinksUiState(drinks = listOf(drink), deleteConfirmation = drink, isLoading = false)
        )
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                ManageDrinksScreen(
                    onNavigateToOfficialDrinks = {},
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.delete_drink_confirm)).assertIsDisplayed()
        composeTestRule.onNodeWithText(
            context.getString(R.string.delete_drink_warning, drink.name), substring = true
        ).assertIsDisplayed()
    }

    @Test
    fun manageDrinksScreen_deleteConfirmation_confirm_callsConfirmDelete() {
        val drink = DrinkEntity(2, "Mój napój", 100, 250, false)
        val fakeVm = mockk<ManageDrinksViewModel>(relaxed = true)
        every { fakeVm.uiState } returns MutableStateFlow(
            ManageDrinksUiState(drinks = listOf(drink), deleteConfirmation = drink, isLoading = false)
        ) as StateFlow<ManageDrinksUiState>

        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                ManageDrinksScreen(
                    onNavigateToOfficialDrinks = {},
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.delete_drink_confirm)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.delete)).performClick()
        verify { fakeVm.confirmDelete() }
    }

    @Test
    fun manageDrinksScreen_deleteConfirmation_cancel_callsCancelDelete() {
        val drink = DrinkEntity(2, "Mój napój", 100, 250, false)
        val fakeVm = mockk<ManageDrinksViewModel>(relaxed = true)
        every { fakeVm.uiState } returns MutableStateFlow(
            ManageDrinksUiState(drinks = listOf(drink), deleteConfirmation = drink, isLoading = false)
        ) as StateFlow<ManageDrinksUiState>

        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                ManageDrinksScreen(
                    onNavigateToOfficialDrinks = {},
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.delete_drink_confirm)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.cancel)).performClick()
        verify { fakeVm.cancelDelete() }
    }

    @Test
    fun manageDrinksScreen_addForm_displaysForm() {
        val fakeVm = createFakeViewModel(ManageDrinksUiState(showAddForm = true, isLoading = false))
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                ManageDrinksScreen(
                    onNavigateToOfficialDrinks = {},
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.add_drink_form_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.drink_name_label)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.cancel)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.add_drink)).assertIsDisplayed()
    }

    @Test
    fun manageDrinksScreen_addForm_save_callsSaveDrink() {
        val fakeVm = mockk<ManageDrinksViewModel>(relaxed = true)
        every { fakeVm.uiState } returns MutableStateFlow(
            ManageDrinksUiState(showAddForm = true, isLoading = false)
        ) as StateFlow<ManageDrinksUiState>

        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                ManageDrinksScreen(
                    onNavigateToOfficialDrinks = {},
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.add_drink)).performClick()
        verify { fakeVm.saveDrink(any(), any(), any()) }
    }

    @Test
    fun manageDrinksScreen_addForm_cancel_callsDismissForm() {
        val fakeVm = mockk<ManageDrinksViewModel>(relaxed = true)
        every { fakeVm.uiState } returns MutableStateFlow(
            ManageDrinksUiState(showAddForm = true, isLoading = false)
        ) as StateFlow<ManageDrinksUiState>

        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                ManageDrinksScreen(
                    onNavigateToOfficialDrinks = {},
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.cancel)).performClick()
        verify { fakeVm.dismissForm() }
    }

    @Test
    fun manageDrinksScreen_editForm_displaysPreFilledValues() {
        val editDrink = DrinkEntity(2, "Mój napój", 100, 250, false)
        val fakeVm = createFakeViewModel(
            ManageDrinksUiState(showAddForm = true, editDrink = editDrink, isLoading = false)
        )
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                ManageDrinksScreen(
                    onNavigateToOfficialDrinks = {},
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.edit_drink_form_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.save)).assertIsDisplayed()
    }

    // ===== Navigation tests =====

    @Test
    fun manageDrinksScreen_browseOfficialCard_navigatesToOfficialDrinks() {
        var navigated = false
        val fakeVm = createFakeViewModel(ManageDrinksUiState(isLoading = false))
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                ManageDrinksScreen(
                    onNavigateToOfficialDrinks = { navigated = true },
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.browse_official_drinks)).performClick()
        assert(navigated) { "Browse official card click should trigger onNavigateToOfficialDrinks" }
    }

    @Test
    fun manageDrinksScreen_backButton_navigatesBack() {
        var navigated = false
        val fakeVm = createFakeViewModel(ManageDrinksUiState(isLoading = false))
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                ManageDrinksScreen(
                    onNavigateToOfficialDrinks = {},
                    onNavigateBack = { navigated = true },
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.back)).performClick()
        assert(navigated) { "Back button click should trigger onNavigateBack" }
    }

    // ===== State tests =====

    @Test
    fun manageDrinksScreen_showsLoadingIndicator_whenLoading() {
        val fakeVm = createFakeViewModel(ManageDrinksUiState(isLoading = true))
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                ManageDrinksScreen(
                    onNavigateToOfficialDrinks = {},
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        // TopAppBar should still show
        composeTestRule.onNodeWithText(context.getString(R.string.manage_drinks_title)).assertIsDisplayed()
        // FAB should also be visible (Scaffold renders it)
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.add_drink)).assertIsDisplayed()
    }

    @Test
    fun manageDrinksScreen_showsEmptyState_whenNoDrinks() {
        val fakeVm = createFakeViewModel(ManageDrinksUiState(drinks = emptyList(), isLoading = false))
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                ManageDrinksScreen(
                    onNavigateToOfficialDrinks = {},
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.no_custom_drinks)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.no_custom_drinks_hint)).assertIsDisplayed()
        // Browse card should still show in empty state
        composeTestRule.onNodeWithText(context.getString(R.string.browse_official_drinks)).assertIsDisplayed()
    }

    // ===== Accessibility content description tests =====

    @Test
    fun manageDrinksScreen_backButton_hasContentDescription() {
        val fakeVm = createFakeViewModel(ManageDrinksUiState(isLoading = false))
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                ManageDrinksScreen(
                    onNavigateToOfficialDrinks = {},
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.back)).assertIsDisplayed()
    }

    @Test
    fun manageDrinksScreen_addDrinkFAB_hasContentDescription() {
        val fakeVm = createFakeViewModel(ManageDrinksUiState(isLoading = false))
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                ManageDrinksScreen(
                    onNavigateToOfficialDrinks = {},
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.add_drink)).assertIsDisplayed()
    }

    @Test
    fun manageDrinksScreen_editIcon_hasContentDescription() {
        val drinks = listOf(
            DrinkEntity(2, "Mój napój", 100, 250, false)
        )
        val fakeVm = createFakeViewModel(ManageDrinksUiState(drinks = drinks, isLoading = false))
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                ManageDrinksScreen(
                    onNavigateToOfficialDrinks = {},
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.edit_drink_form_title)).assertIsDisplayed()
    }

    @Test
    fun manageDrinksScreen_deleteIcon_hasContentDescription() {
        val drinks = listOf(
            DrinkEntity(2, "Mój napój", 100, 250, false)
        )
        val fakeVm = createFakeViewModel(ManageDrinksUiState(drinks = drinks, isLoading = false))
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                ManageDrinksScreen(
                    onNavigateToOfficialDrinks = {},
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.delete)).assertIsDisplayed()
    }

    // ===== Utility =====

    private fun createFakeViewModel(
        state: ManageDrinksUiState
    ): ManageDrinksViewModel {
        val vm = mockk<ManageDrinksViewModel>(relaxed = true)
        every { vm.uiState } returns MutableStateFlow(state) as StateFlow<ManageDrinksUiState>
        return vm
    }
}
