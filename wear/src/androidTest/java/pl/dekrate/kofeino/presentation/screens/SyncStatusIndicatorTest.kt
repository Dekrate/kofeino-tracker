package pl.dekrate.kofeino.presentation.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
}
