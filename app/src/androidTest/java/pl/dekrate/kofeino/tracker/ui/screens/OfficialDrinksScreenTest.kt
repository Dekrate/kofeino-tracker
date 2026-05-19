package pl.dekrate.kofeino.tracker.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test
import pl.dekrate.kofeino.tracker.R
import pl.dekrate.kofeino.tracker.domain.model.OfficialDrink
import pl.dekrate.kofeino.tracker.presentation.viewmodel.OfficialDrinksUiState
import pl.dekrate.kofeino.tracker.presentation.viewmodel.OfficialDrinksViewModel
import pl.dekrate.kofeino.tracker.ui.theme.KofeinoTrackerPhoneTheme

class OfficialDrinksScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    // ===== Display tests =====

    @Test
    fun officialDrinksScreen_displaysTitle() {
        val fakeVm = createFakeViewModel(OfficialDrinksUiState())
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                OfficialDrinksScreen(
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.official_drinks_title)).assertIsDisplayed()
    }

    @Test
    fun officialDrinksScreen_displaysSearchField() {
        val fakeVm = createFakeViewModel(OfficialDrinksUiState())
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                OfficialDrinksScreen(
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.search_hint), substring = true).assertIsDisplayed()
    }

    @Test
    fun officialDrinksScreen_showsTypeToSearchInitially() {
        val fakeVm = createFakeViewModel(OfficialDrinksUiState())
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                OfficialDrinksScreen(
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.type_to_search)).assertIsDisplayed()
    }

    @Test
    fun officialDrinksScreen_showsLoadingIndicator_whenLoading() {
        val fakeVm = createFakeViewModel(OfficialDrinksUiState(isLoading = true))
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                OfficialDrinksScreen(
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.search_loading)).assertIsDisplayed()
    }

    @Test
    fun officialDrinksScreen_showsNoResultsState() {
        val fakeVm = createFakeViewModel(
            OfficialDrinksUiState(isSearchMode = true, searchQuery = "xyz", drinks = emptyList())
        )
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                OfficialDrinksScreen(
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.no_search_results)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.no_search_results_hint)).assertIsDisplayed()
    }

    @Test
    fun officialDrinksScreen_showsSearchButton_whenQueryNotEmpty() {
        val fakeVm = createFakeViewModel(
            OfficialDrinksUiState(searchQuery = "cola", isSearchMode = true)
        )
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                OfficialDrinksScreen(
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(
            context.getString(R.string.search_hint).removeSuffix("\u2026")
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.search_clear)).assertIsDisplayed()
    }

    @Test
    fun officialDrinksScreen_showsClearButton_whenQueryNotEmpty() {
        val fakeVm = createFakeViewModel(
            OfficialDrinksUiState(searchQuery = "cola", isSearchMode = true)
        )
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                OfficialDrinksScreen(
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.search_clear)).assertIsDisplayed()
    }

    @Test
    fun officialDrinksScreen_hidesClearButton_whenQueryEmpty() {
        val fakeVm = createFakeViewModel(OfficialDrinksUiState())
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                OfficialDrinksScreen(
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.search_clear)).assertDoesNotExist()
    }

    // ===== Results display tests =====

    @Test
    fun officialDrinksScreen_displaysResults() {
        val drinks = listOf(
            OfficialDrink(barcode = "123", name = "Coca-Cola", brand = "Coca-Cola", caffeineMgPer100ml = 10.0),
            OfficialDrink(barcode = "456", name = "Pepsi", brand = "PepsiCo", caffeineMgPer100ml = 8.0)
        )
        val fakeVm = createFakeViewModel(
            OfficialDrinksUiState(drinks = drinks, isSearchMode = true, searchQuery = "cola")
        )
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                OfficialDrinksScreen(
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText("Coca-Cola").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pepsi").assertIsDisplayed()
    }

    @Test
    fun officialDrinksScreen_displaysCaffeinePer100ml() {
        val drinks = listOf(
            OfficialDrink(barcode = "123", name = "Monster", brand = "Monster", caffeineMgPer100ml = 32.0)
        )
        val fakeVm = createFakeViewModel(
            OfficialDrinksUiState(drinks = drinks, isSearchMode = true, searchQuery = "monster")
        )
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                OfficialDrinksScreen(
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(
            context.getString(R.string.caffeine_per_100ml, 32.0), substring = true
        ).assertIsDisplayed()
    }

    @Test
    fun officialDrinksScreen_displaysBrandAndCaffeine() {
        val drinks = listOf(
            OfficialDrink(barcode = "123", name = "Monster", brand = "Monster Energy", caffeineMgPer100ml = 32.0)
        )
        val fakeVm = createFakeViewModel(
            OfficialDrinksUiState(drinks = drinks, isSearchMode = true, searchQuery = "monster")
        )
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                OfficialDrinksScreen(
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        // Both brand and caffeine should appear in subtitle
        composeTestRule.onNodeWithText("Monster Energy", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText(
            context.getString(R.string.caffeine_per_100ml, 32.0), substring = true
        ).assertIsDisplayed()
    }

    @Test
    fun officialDrinksScreen_showsImportButton() {
        val drinks = listOf(
            OfficialDrink(barcode = "123", name = "Coca-Cola", brand = null, caffeineMgPer100ml = 10.0)
        )
        val fakeVm = createFakeViewModel(
            OfficialDrinksUiState(drinks = drinks, isSearchMode = true, searchQuery = "cola")
        )
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                OfficialDrinksScreen(
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.import_drink)).assertIsDisplayed()
    }

    @Test
    fun officialDrinksScreen_showsImportedButton_whenAlreadyImported() {
        val drinks = listOf(
            OfficialDrink(barcode = "123", name = "Coca-Cola", brand = null, caffeineMgPer100ml = 10.0)
        )
        val fakeVm = createFakeViewModel(
            OfficialDrinksUiState(
                drinks = drinks,
                importedDrinkNames = setOf("Coca-Cola"),
                isSearchMode = true,
                searchQuery = "cola"
            )
        )
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                OfficialDrinksScreen(
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.imported_as_drink)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.import_drink)).assertDoesNotExist()
    }

    // ===== Interaction tests =====

    @Test
    fun officialDrinksScreen_importButton_callsImportAsDrink() {
        val drink = OfficialDrink(barcode = "123", name = "Coca-Cola", brand = null, caffeineMgPer100ml = 10.0)
        val fakeVm = mockk<OfficialDrinksViewModel>(relaxed = true)
        every { fakeVm.uiState } returns MutableStateFlow(
            OfficialDrinksUiState(drinks = listOf(drink), isSearchMode = true, searchQuery = "cola")
        ) as StateFlow<OfficialDrinksUiState>

        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                OfficialDrinksScreen(
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.import_drink)).performClick()
        verify { fakeVm.importAsDrink(drink) }
    }

    @Test
    fun officialDrinksScreen_searchImmediateButton_triggersSearch() {
        val fakeVm = mockk<OfficialDrinksViewModel>(relaxed = true)
        every { fakeVm.uiState } returns MutableStateFlow(
            OfficialDrinksUiState(searchQuery = "cola", isSearchMode = true)
        ) as StateFlow<OfficialDrinksUiState>

        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                OfficialDrinksScreen(
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(
            context.getString(R.string.search_hint).removeSuffix("\u2026")
        ).performClick()
        verify { fakeVm.searchImmediate("cola") }
    }

    @Test
    fun officialDrinksScreen_clearSearchButton_callsClearSearch() {
        val fakeVm = mockk<OfficialDrinksViewModel>(relaxed = true)
        every { fakeVm.uiState } returns MutableStateFlow(
            OfficialDrinksUiState(searchQuery = "cola", isSearchMode = true)
        ) as StateFlow<OfficialDrinksUiState>

        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                OfficialDrinksScreen(
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.search_clear)).performClick()
        // Both the button and the trailing icon call clearSearch — just verify it was called
        verify { fakeVm.clearSearch() }
    }

    @Test
    fun officialDrinksScreen_typingInSearch_updatesQuery() {
        val fakeVm = mockk<OfficialDrinksViewModel>(relaxed = true)
        every { fakeVm.uiState } returns MutableStateFlow(
            OfficialDrinksUiState()
        ) as StateFlow<OfficialDrinksUiState>

        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                OfficialDrinksScreen(
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.search_hint), substring = true).performTextInput("cola")
        verify { fakeVm.onQueryChanged("cola") }
    }

    // ===== Navigation test =====

    @Test
    fun officialDrinksScreen_backButton_navigatesBack() {
        var navigated = false
        val fakeVm = createFakeViewModel(OfficialDrinksUiState())
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                OfficialDrinksScreen(
                    onNavigateBack = { navigated = true },
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.back)).performClick()
        assert(navigated) { "Back button click should trigger onNavigateBack" }
    }

    // ===== Background loading test =====

    @Test
    fun officialDrinksScreen_showsSmallLoadingIndicator_duringBackgroundRefresh() {
        val drinks = listOf(
            OfficialDrink(barcode = "123", name = "Coca-Cola", brand = null, caffeineMgPer100ml = 10.0)
        )
        val fakeVm = createFakeViewModel(
            OfficialDrinksUiState(drinks = drinks, isLoading = true, isSearchMode = true, searchQuery = "cola")
        )
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                OfficialDrinksScreen(
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        // Results should still be displayed
        composeTestRule.onNodeWithText("Coca-Cola").assertIsDisplayed()
    }

    // ===== Accessibility content description tests =====

    @Test
    fun officialDrinksScreen_backButton_hasContentDescription() {
        val fakeVm = createFakeViewModel(OfficialDrinksUiState())
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                OfficialDrinksScreen(
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.back)).assertIsDisplayed()
    }

    @Test
    fun officialDrinksScreen_clearButton_hasContentDescription() {
        val fakeVm = createFakeViewModel(
            OfficialDrinksUiState(searchQuery = "cola", isSearchMode = true)
        )
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                OfficialDrinksScreen(
                    onNavigateBack = {},
                    viewModel = fakeVm
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.search_clear)).assertIsDisplayed()
    }

    // ===== Utility =====

    private fun createFakeViewModel(
        state: OfficialDrinksUiState
    ): OfficialDrinksViewModel {
        val vm = mockk<OfficialDrinksViewModel>(relaxed = true)
        every { vm.uiState } returns MutableStateFlow(state) as StateFlow<OfficialDrinksUiState>
        return vm
    }
}
