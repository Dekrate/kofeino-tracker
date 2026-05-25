package pl.dekrate.kofeino.presentation.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import pl.dekrate.kofeino.common.sync.SyncStatus
import pl.dekrate.kofeino.data.sync.SyncStatusTracker
import pl.dekrate.kofeino.presentation.theme.KofeinoTrackerTheme

class SyncStatusIndicatorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createMockTracker(initialStatus: SyncStatus): SyncStatusTracker {
        val statusFlow = MutableStateFlow(initialStatus)
        val tracker = mockk<SyncStatusTracker>(relaxed = true)
        every { tracker.status } returns statusFlow
        return tracker
    }

    // ── Existing tests (basic visibility per state) ──────────────────────────

    @Test
    fun syncStatusIndicator_isNotDisplayed_whenSynced() {
        val tracker = createMockTracker(SyncStatus.Synced)
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                SyncStatusIndicator(syncStatusTracker = tracker)
            }
        }
        // When Synced, isVisible = false so the Box is never composed
        composeTestRule.onNodeWithTag("sync_status_indicator").assertDoesNotExist()
    }

    @Test
    fun syncStatusIndicator_isDisplayed_whenAwaitingDevice() {
        val tracker = createMockTracker(SyncStatus.AwaitingDevice)
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                SyncStatusIndicator(syncStatusTracker = tracker)
            }
        }
        composeTestRule.onNodeWithTag("sync_status_indicator").assertIsDisplayed()
    }

    @Test
    fun syncStatusIndicator_isDisplayed_whenSyncing() {
        val tracker = createMockTracker(SyncStatus.Syncing)
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                SyncStatusIndicator(syncStatusTracker = tracker)
            }
        }
        composeTestRule.onNodeWithTag("sync_status_indicator").assertIsDisplayed()
    }

    @Test
    fun syncStatusIndicator_isDisplayed_whenError() {
        val tracker = createMockTracker(SyncStatus.Error("test error"))
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                SyncStatusIndicator(syncStatusTracker = tracker)
            }
        }
        composeTestRule.onNodeWithTag("sync_status_indicator").assertIsDisplayed()
    }

    // ── Content description tests ────────────────────────────────────────────

    @Test
    fun syncStatusIndicator_hasCorrectContentDescription_whenError() {
        val tracker = createMockTracker(SyncStatus.Error("test error"))
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                SyncStatusIndicator(syncStatusTracker = tracker)
            }
        }
        composeTestRule
            .onNodeWithContentDescription("Sync error")
            .assertIsDisplayed()
    }

    @Test
    fun syncStatusIndicator_hasCorrectContentDescription_whenSyncing() {
        val tracker = createMockTracker(SyncStatus.Syncing)
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                SyncStatusIndicator(syncStatusTracker = tracker)
            }
        }
        composeTestRule
            .onNodeWithContentDescription("Syncing")
            .assertIsDisplayed()
    }

    @Test
    fun syncStatusIndicator_hasCorrectContentDescription_whenAwaitingDevice() {
        val tracker = createMockTracker(SyncStatus.AwaitingDevice)
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                SyncStatusIndicator(syncStatusTracker = tracker)
            }
        }
        composeTestRule
            .onNodeWithContentDescription("Waiting for device")
            .assertIsDisplayed()
    }

    // ── Error interaction test ───────────────────────────────────────────────

    @Test
    fun syncStatusIndicator_isClickable_whenError() {
        val tracker = createMockTracker(SyncStatus.Error("test error"))
        composeTestRule.setContent {
            KofeinoTrackerTheme {
                SyncStatusIndicator(syncStatusTracker = tracker)
            }
        }
        // The error dot is tappable and triggers haptic feedback.
        // Haptic feedback cannot be verified in unit tests, but we verify the
        // click action is wired up (does not throw).
        composeTestRule
            .onNodeWithTag("sync_status_indicator")
            .performClick()
            .assertIsDisplayed()
    }

    // ── State transition test ────────────────────────────────────────────────

    @Test
    fun syncStatusIndicator_transitionsBetweenStates() {
        val statusFlow = MutableStateFlow<SyncStatus>(SyncStatus.AwaitingDevice)
        val tracker = mockk<SyncStatusTracker>(relaxed = true)
        every { tracker.status } returns statusFlow

        composeTestRule.setContent {
            KofeinoTrackerTheme {
                SyncStatusIndicator(syncStatusTracker = tracker)
            }
        }

        // Start with AwaitingDevice → visible
        composeTestRule.onNodeWithTag("sync_status_indicator").assertIsDisplayed()

        // Transition to Synced → should disappear
        statusFlow.value = SyncStatus.Synced
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("sync_status_indicator").assertDoesNotExist()

        // Transition back to AwaitingDevice → should reappear
        statusFlow.value = SyncStatus.AwaitingDevice
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("sync_status_indicator").assertIsDisplayed()
    }
}
