package pl.dekrate.kofeino.tracker.data.sync.adb

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import timber.log.Timber

/**
 * Debug-only ContentProvider that eagerly initializes the ADB sync bridge.
 *
 * Works by accessing [AdbSyncServer] through a Hilt [EntryPoint], which
 * triggers its `init` block and starts the TCP server on port 8888.
 *
 * Registered in `app/src/debug/AndroidManifest.xml` — only present in
 * debug builds, zero impact on release builds.
 */
class DebugBridgeInitializer : ContentProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BridgeEntryPoint {
        fun adbSyncServer(): AdbSyncServer
    }

    override fun onCreate(): Boolean {
        Timber.i("DebugBridgeInitializer: eagerly initializing AdbSyncServer")
        val entryPoint = EntryPointAccessors.fromApplication(
            requireNotNull(context) { "Context must not be null in ContentProvider.onCreate()" },
            BridgeEntryPoint::class.java
        )
        entryPoint.adbSyncServer()
        Timber.i("DebugBridgeInitializer: AdbSyncServer instantiated and started")
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun getType(uri: Uri): String? = null
}
