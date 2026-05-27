package pl.dekrate.kofeino.tracker.data.sync.adb

import android.content.Context
import android.net.Uri
import com.google.android.gms.common.api.GoogleApi
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.Node
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdbCapabilityClient @Inject constructor(
    @ApplicationContext context: Context
) : CapabilityClient(context, GoogleApi.Settings.Builder().build()) {

    companion object {
        const val ADB_NODE_ID = "adb-bridge"
        const val SYNC_CAPABILITY_NAME = "caffeine_sync"
    }

    private val listeners = CopyOnWriteArrayList<CapabilityClient.OnCapabilityChangedListener>()
    @Volatile private var connected: Boolean = false

    private val adbNode = object : Node {
        override fun getId() = ADB_NODE_ID
        override fun getDisplayName() = "ADB Bridge (debug)"
        override fun isNearby() = connected
    }

    private fun createCapabilityInfo(name: String): CapabilityInfo {
        return object : CapabilityInfo {
            override fun getName() = name
            override fun getNodes(): Set<Node> = if (connected) setOf(adbNode) else emptySet()
        }
    }

    internal fun setConnected(value: Boolean) {
        if (connected == value) return
        connected = value
        val info = createCapabilityInfo(SYNC_CAPABILITY_NAME)
        for (l in listeners) {
            try {
                l.onCapabilityChanged(info)
            } catch (e: Exception) {
                Timber.w(e, "AdbCapabilityClient: listener threw")
            }
        }
    }

    override fun getCapability(name: String, filter: Int): Task<CapabilityInfo> =
        Tasks.forResult(createCapabilityInfo(name))

    override fun getAllCapabilities(filter: Int): Task<Map<String, CapabilityInfo>> =
        Tasks.forResult(mapOf(SYNC_CAPABILITY_NAME to createCapabilityInfo(SYNC_CAPABILITY_NAME)))

    override fun addListener(
        listener: CapabilityClient.OnCapabilityChangedListener,
        name: String
    ): Task<Void> {
        listeners.add(listener); return Tasks.forResult(null)
    }

    override fun addListener(
        listener: CapabilityClient.OnCapabilityChangedListener,
        uri: Uri,
        strategy: Int
    ): Task<Void> {
        listeners.add(listener); return Tasks.forResult(null)
    }

    override fun removeListener(
        listener: CapabilityClient.OnCapabilityChangedListener
    ): Task<Boolean> {
        listeners.remove(listener); return Tasks.forResult(true)
    }

    override fun removeListener(
        listener: CapabilityClient.OnCapabilityChangedListener,
        name: String
    ): Task<Boolean> {
        listeners.remove(listener); return Tasks.forResult(true)
    }

    override fun addLocalCapability(name: String): Task<Void> = Tasks.forResult(null)

    override fun removeLocalCapability(name: String): Task<Void> = Tasks.forResult(null)
}
