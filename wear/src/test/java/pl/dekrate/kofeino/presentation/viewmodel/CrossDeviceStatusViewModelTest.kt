package pl.dekrate.kofeino.presentation.viewmodel

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import app.cash.turbine.test
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.Node
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import pl.dekrate.kofeino.common.sync.CrossDeviceStatus
import pl.dekrate.kofeino.common.sync.SyncStatus
import pl.dekrate.kofeino.data.sync.ConflictLogDao
import pl.dekrate.kofeino.data.sync.PendingChangeDao
import pl.dekrate.kofeino.data.sync.SyncStatusTracker
import pl.dekrate.kofeino.data.sync.WearableDataLayerManager
import pl.dekrate.kofeino.data.sync.ioCoroutineDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
class CrossDeviceStatusViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = object : TestWatcher() {
        override fun starting(description: Description?) {
            Dispatchers.setMain(testDispatcher)
        }
        override fun finished(description: Description?) {
            Dispatchers.resetMain()
        }
    }

    // ── Mocks ──
    private lateinit var syncStatusTracker: SyncStatusTracker
    private lateinit var pendingChangeDao: PendingChangeDao
    private lateinit var conflictLogDao: ConflictLogDao
    private lateinit var capabilityClient: CapabilityClient
    private lateinit var wearableDataLayerManager: WearableDataLayerManager
    private lateinit var context: Context
    private lateinit var viewModel: CrossDeviceStatusViewModel

    private val syncStatusFlow = MutableStateFlow(SyncStatus.initial)
    private val pendingCountFlow = MutableStateFlow(0)
    private val failedCountFlow = MutableStateFlow(0)
    private val conflictCountFlow = MutableStateFlow(0)

    @Before
    fun setUp() {
        ioCoroutineDispatcher = testDispatcher
        syncStatusTracker = mockk()
        pendingChangeDao = mockk()
        conflictLogDao = mockk()
        capabilityClient = mockk()
        wearableDataLayerManager = mockk()
        context = mockk()

        every { syncStatusTracker.status } returns syncStatusFlow
        every { pendingChangeDao.observeCount() } returns pendingCountFlow
        every { pendingChangeDao.observeFailedCount(any<String>()) } returns failedCountFlow
        coEvery { pendingChangeDao.getLatestEnqueuedTimestamp() } returns 1234567890L
        every { conflictLogDao.observeCount() } returns conflictCountFlow

        val packageManager = mockk<PackageManager>()
        val packageInfo = PackageInfo().apply { versionName = "2.0.0" }
        every { context.packageManager } returns packageManager
        every { context.packageName } returns "pl.dekrate.kofeino"
        every { packageManager.getPackageInfo(any<String>(), any<Int>()) } returns packageInfo
    }

    @After
    fun tearDown() {
        ioCoroutineDispatcher = kotlinx.coroutines.Dispatchers.IO
        Dispatchers.resetMain()
    }

    // ── Helpers ──

    /** Sets up CapabilityClient to simulate device connections. */
    private fun setupCapability(hasDevice: Boolean, displayName: String? = "Phone") {
        if (hasDevice) {
            val node = mockk<Node>()
            every { node.displayName } returns (displayName ?: "Unknown")
            every { node.id } returns "node-1"
            val capabilityInfo = mockk<CapabilityInfo>()
            every { capabilityInfo.nodes } returns setOf(node)
            every { capabilityClient.getCapability(any(), any()) } returns Tasks.forResult<CapabilityInfo>(capabilityInfo)
        } else {
            val capabilityInfo = mockk<CapabilityInfo>()
            every { capabilityInfo.nodes } returns emptySet()
            every { capabilityClient.getCapability(any(), any()) } returns Tasks.forResult<CapabilityInfo>(capabilityInfo)
        }
    }

    // ── Initial state tests ──

    @Test
    fun `initial state should use SyncStatus initial`() = runTest {
        setupCapability(hasDevice = true)
        viewModel = CrossDeviceStatusViewModel(
            syncStatusTracker, pendingChangeDao, conflictLogDao,
            capabilityClient, wearableDataLayerManager, context
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SyncStatus.initial, state.syncStatus)
    }

    @Test
    fun `initial state should reflect paired device info`() = runTest {
        setupCapability(hasDevice = true, displayName = "Pixel 8")
        viewModel = CrossDeviceStatusViewModel(
            syncStatusTracker, pendingChangeDao, conflictLogDao,
            capabilityClient, wearableDataLayerManager, context
        )
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue("isPaired should be true", state.deviceStatus.isPaired)
        assertEquals("Pixel 8", state.deviceStatus.pairedDeviceName)
    }

    @Test
    fun `initial state should show no paired device when alone`() = runTest {
        setupCapability(hasDevice = false)
        viewModel = CrossDeviceStatusViewModel(
            syncStatusTracker, pendingChangeDao, conflictLogDao,
            capabilityClient, wearableDataLayerManager, context
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse("isPaired should be false", state.deviceStatus.isPaired)
        assertEquals(null, state.deviceStatus.pairedDeviceName)
    }

    @Test
    fun `initial state should show app version`() = runTest {
        setupCapability(hasDevice = true)
        viewModel = CrossDeviceStatusViewModel(
            syncStatusTracker, pendingChangeDao, conflictLogDao,
            capabilityClient, wearableDataLayerManager, context
        )
        advanceUntilIdle()
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("2.0.0", state.deviceStatus.localAppVersion)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── SyncStatus reactive tests ──

    @Test
    fun `uiState should emit new syncStatus when syncStatusTracker changes`() = runTest {
        setupCapability(hasDevice = true)
        viewModel = CrossDeviceStatusViewModel(
            syncStatusTracker, pendingChangeDao, conflictLogDao,
            capabilityClient, wearableDataLayerManager, context
        )
        advanceUntilIdle()

        viewModel.uiState.test {
            // Skip initial emission
            var state = awaitItem()
            assertEquals(SyncStatus.initial, state.syncStatus)

            syncStatusFlow.value = SyncStatus.Synced
            advanceUntilIdle()
            state = awaitItem()
            assertEquals(SyncStatus.Synced, state.syncStatus)

            syncStatusFlow.value = SyncStatus.Syncing
            advanceUntilIdle()
            state = awaitItem()
            assertEquals(SyncStatus.Syncing, state.syncStatus)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `uiState should reflect sync error status`() = runTest {
        setupCapability(hasDevice = true)
        viewModel = CrossDeviceStatusViewModel(
            syncStatusTracker, pendingChangeDao, conflictLogDao,
            capabilityClient, wearableDataLayerManager, context
        )
        viewModel.uiState.test {
            // Wait for explicit initial + combine initial, then set error
            skipItems(2)
            syncStatusFlow.value = SyncStatus.Error("Watch unreachable")
            val state = awaitItem()
            assertTrue(state.syncStatus is SyncStatus.Error)
            assertEquals("Watch unreachable", (state.syncStatus as SyncStatus.Error).message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Queue depth tests ──

    @Test
    fun `should reflect pending change count`() = runTest {
        setupCapability(hasDevice = true)
        viewModel = CrossDeviceStatusViewModel(
            syncStatusTracker, pendingChangeDao, conflictLogDao,
            capabilityClient, wearableDataLayerManager, context
        )
        advanceUntilIdle()

        viewModel.uiState.test {
            skipItems(1)

            pendingCountFlow.value = 5
            advanceUntilIdle()
            val state = awaitItem()
            assertEquals(5, state.deviceStatus.pendingChangeCount)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should reflect failed change count`() = runTest {
        setupCapability(hasDevice = true)
        viewModel = CrossDeviceStatusViewModel(
            syncStatusTracker, pendingChangeDao, conflictLogDao,
            capabilityClient, wearableDataLayerManager, context
        )
        advanceUntilIdle()

        viewModel.uiState.test {
            skipItems(1)

            failedCountFlow.value = 2
            advanceUntilIdle()
            val state = awaitItem()
            assertEquals(2, state.deviceStatus.failedChangeCount)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should reflect conflict log count`() = runTest {
        setupCapability(hasDevice = true)
        viewModel = CrossDeviceStatusViewModel(
            syncStatusTracker, pendingChangeDao, conflictLogDao,
            capabilityClient, wearableDataLayerManager, context
        )
        advanceUntilIdle()

        viewModel.uiState.test {
            skipItems(1)

            conflictCountFlow.value = 1
            advanceUntilIdle()
            val state = awaitItem()
            assertEquals(1, state.deviceStatus.conflictLogCount)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Edge cases ──

    @Test
    fun `should handle capability client exception gracefully`() = runTest {
        every { capabilityClient.getCapability(any(), any()) } throws RuntimeException("Timeout")
        viewModel = CrossDeviceStatusViewModel(
            syncStatusTracker, pendingChangeDao, conflictLogDao,
            capabilityClient, wearableDataLayerManager, context
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse("isPaired should be false on exception", state.deviceStatus.isPaired)
        assertEquals(null, state.deviceStatus.pairedDeviceName)
    }

    @Test
    fun `should handle null versionName from package manager`() = runTest {
        setupCapability(hasDevice = true)
        val packageManager = mockk<PackageManager>()
        val packageInfo = PackageInfo().apply {
            versionName = null
        }
        every { context.packageManager } returns packageManager
        every { context.packageName } returns "pl.dekrate.kofeino"
        every { packageManager.getPackageInfo(any<String>(), any<Int>()) } returns packageInfo

        viewModel = CrossDeviceStatusViewModel(
            syncStatusTracker, pendingChangeDao, conflictLogDao,
            capabilityClient, wearableDataLayerManager, context
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.deviceStatus.localAppVersion)
    }

    @Test
    fun `should handle PackageManager NameNotFoundException`() = runTest {
        setupCapability(hasDevice = true)
        val packageManager = mockk<PackageManager>()
        every { context.packageManager } returns packageManager
        every { context.packageName } returns "pl.dekrate.kofeino"
        every { packageManager.getPackageInfo(any<String>(), any<Int>()) } throws PackageManager.NameNotFoundException()

        viewModel = CrossDeviceStatusViewModel(
            syncStatusTracker, pendingChangeDao, conflictLogDao,
            capabilityClient, wearableDataLayerManager, context
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.deviceStatus.localAppVersion)
    }

    @Test
    fun `should combine all sources into one state`() = runTest {
        setupCapability(hasDevice = true, displayName = "Pixel 8 Pro")
        pendingCountFlow.value = 3
        failedCountFlow.value = 1
        conflictCountFlow.value = 2
        syncStatusFlow.value = SyncStatus.Synced

        viewModel = CrossDeviceStatusViewModel(
            syncStatusTracker, pendingChangeDao, conflictLogDao,
            capabilityClient, wearableDataLayerManager, context
        )
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue(state.deviceStatus.isPaired)
        assertEquals("Pixel 8 Pro", state.deviceStatus.pairedDeviceName)
        assertEquals(3, state.deviceStatus.pendingChangeCount)
        assertEquals(1, state.deviceStatus.failedChangeCount)
        assertEquals(2, state.deviceStatus.conflictLogCount)
        assertEquals(SyncStatus.Synced, state.syncStatus)
        assertEquals("2.0.0", state.deviceStatus.localAppVersion)
    }
}
