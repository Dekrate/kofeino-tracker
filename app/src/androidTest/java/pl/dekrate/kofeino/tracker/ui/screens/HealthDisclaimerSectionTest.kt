package pl.dekrate.kofeino.tracker.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import pl.dekrate.kofeino.tracker.R
import pl.dekrate.kofeino.tracker.ui.theme.KofeinoTrackerPhoneTheme

class HealthDisclaimerSectionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    // ===== Initial State Tests =====

    @Test
    fun displaysDisclaimerHeader() {
        setUpSection()
        composeTestRule.onNodeWithText(context.getString(R.string.health_disclaimer_title))
            .assertIsDisplayed()
    }

    @Test
    fun displaysSourcesHeader() {
        setUpSection()
        composeTestRule.onNodeWithText(context.getString(R.string.health_references_title))
            .assertIsDisplayed()
    }

    // ===== Expand/Collapse Tests =====

    @Test
    fun disclaimerBody_isHiddenInitially() {
        setUpSection()
        composeTestRule.onNodeWithText(context.getString(R.string.health_disclaimer_text))
            .assertDoesNotExist()
    }

    @Test
    fun sourcesBody_isHiddenInitially() {
        setUpSection()
        composeTestRule.onNodeWithText(context.getString(R.string.health_references_text))
            .assertDoesNotExist()
    }

    @Test
    fun clickingDisclaimer_expandsContent() {
        setUpSection()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.health_disclaimer_expand))
            .performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.health_disclaimer_text))
            .assertIsDisplayed()
    }

    @Test
    fun clickingDisclaimerTwice_collapsesContent() {
        setUpSection()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.health_disclaimer_expand))
            .performClick()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.health_disclaimer_collapse))
            .performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.health_disclaimer_text))
            .assertDoesNotExist()
    }

    @Test
    fun clickingSources_expandsContent() {
        setUpSection()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.health_references_expand))
            .performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.health_references_text))
            .assertIsDisplayed()
    }

    @Test
    fun clickingSourcesTwice_collapsesContent() {
        setUpSection()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.health_references_expand))
            .performClick()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.health_references_collapse))
            .performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.health_references_text))
            .assertDoesNotExist()
    }

    // ===== Accessibility Tests =====

    @Test
    fun disclaimerCard_hasExpandDescription_whenCollapsed() {
        setUpSection()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.health_disclaimer_expand))
            .assertIsDisplayed()
    }

    @Test
    fun disclaimerCard_hasCollapseDescription_whenExpanded() {
        setUpSection()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.health_disclaimer_expand))
            .performClick()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.health_disclaimer_collapse))
            .assertIsDisplayed()
    }

    @Test
    fun sourcesCard_hasExpandDescription_whenCollapsed() {
        setUpSection()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.health_references_expand))
            .assertIsDisplayed()
    }

    @Test
    fun sourcesCard_hasCollapseDescription_whenExpanded() {
        setUpSection()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.health_references_expand))
            .performClick()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.health_references_collapse))
            .assertIsDisplayed()
    }

    // ===== Text Content Tests =====

    @Test
    fun disclaimerBody_containsFullText() {
        setUpSection()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.health_disclaimer_expand))
            .performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.health_disclaimer_text))
            .assertIsDisplayed()
    }

    @Test
    fun sourcesBody_containsFullText() {
        setUpSection()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.health_references_expand))
            .performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.health_references_text))
            .assertIsDisplayed()
    }

    private fun setUpSection() {
        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                HealthDisclaimerSection()
            }
        }
    }
}
