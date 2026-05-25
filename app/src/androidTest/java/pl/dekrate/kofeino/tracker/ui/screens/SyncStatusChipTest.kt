package pl.dekrate.kofeino.tracker.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import pl.dekrate.kofeino.common.sync.SyncStatus
import pl.dekrate.kofeino.tracker.R
import pl.dekrate.kofeino.tracker.data.sync.SyncStatusTracker
import pl.dekrate.kofeino.tracker.ui.theme.KofeinoTrackerPhoneTheme

/**
 * Compose UI rendering tests for [SyncStatusChip].
 *
 * Verifies that each [SyncStatus] variant displays the correct icon and
 * content description, that the error state is tappable, and that the
 * Synced state auto-hides after the configured delay.
 */
class SyncStatusChipTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    // ===== Display tests =====

    @Test
    fun syncStatusChip_displaysDoneIcon_whenSynced() {
        val tracker = mockk<SyncStatusTracker>(relaxed = true)
        val statusFlow = MutableStateFlow(SyncStatus.Synced)
        every { tracker.status } returns statusFlow

        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                SyncStatusChip(syncStatusTracker = tracker)
            }
        }

        composeTestRule.onNodeWithContentDescription(
            context.getString(R.string.sync_status_synced)
        ).assertIsDisplayed()
    }

    @Test
    fun syncStatusChip_displaysCloseIcon_whenAwaitingDevice() {
        val tracker = mockk<SyncStatusTracker>(relaxed = true)
        val statusFlow = MutableStateFlow(SyncStatus.AwaitingDevice)
        every { tracker.status } returns statusFlow

        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                SyncStatusChip(syncStatusTracker = tracker)
            }
        }

        composeTestRule.onNodeWithContentDescription(
            context.getString(R.string.sync_status_awaiting_device)
        ).assertIsDisplayed()
    }

    @Test
    fun syncStatusChip_displaysSpinner_whenSyncing() {
        val tracker = mockk<SyncStatusTracker>(relaxed = true)
        val statusFlow = MutableStateFlow(SyncStatus.Syncing)
        every { tracker.status } returns statusFlow

        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                SyncStatusChip(syncStatusTracker = tracker)
            }
        }

        composeTestRule.onNodeWithContentDescription(
            context.getString(R.string.sync_status_syncing)
        ).assertIsDisplayed()
    }

    @Test
    fun syncStatusChip_displaysWarningIcon_whenError() {
        val tracker = mockk<SyncStatusTracker>(relaxed = true)
        val statusFlow = MutableStateFlow(SyncStatus.Error("test error"))
        every { tracker.status } returns statusFlow

        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                SyncStatusChip(syncStatusTracker = tracker)
            }
        }

        composeTestRule.onNodeWithContentDescription(
            context.getString(R.string.sync_status_error, "test error")
        ).assertIsDisplayed()
    }

    // ===== Interaction tests =====

    @Test
    fun syncStatusChip_errorIcon_isClickable() {
        val tracker = mockk<SyncStatusTracker>(relaxed = true)
        val statusFlow = MutableStateFlow(SyncStatus.Error("test error"))
        every { tracker.status } returns statusFlow

        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                SyncStatusChip(syncStatusTracker = tracker)
            }
        }

        val errorDescription = context.getString(R.string.sync_status_error, "test error")

        composeTestRule.onNodeWithContentDescription(errorDescription).performClick()

        // Click succeeded without crash — verify the node still exists
        composeTestRule.onNodeWithContentDescription(errorDescription).assertIsDisplayed()
    }

    // ===== Auto-hide test =====

    @Test
    fun syncStatusChip_syncedState_autoHides() {
        val tracker = mockk<SyncStatusTracker>(relaxed = true)
        val statusFlow = MutableStateFlow(SyncStatus.Synced)
        every { tracker.status } returns statusFlow

        composeTestRule.setContent {
            KofeinoTrackerPhoneTheme {
                SyncStatusChip(syncStatusTracker = tracker)
            }
        }

        val syncedDescription = context.getString(R.string.sync_status_synced)

        // Initially visible
        composeTestRule.onNodeWithContentDescription(syncedDescription).assertIsDisplayed()

        // Advance past the 3s auto-hide delay (with buffer for exit animation)
        composeTestRule.mainClock.advanceTimeBy(3500L)

        // Should no longer be present in the composition
        composeTestRule.onNodeWithContentDescription(syncedDescription).assertDoesNotExist()
    }
}
