package pl.dekrate.kofeino.tracker.data.sync.adb

import android.content.Context
import android.net.Uri
import com.google.android.gms.common.api.GoogleApi
import com.google.android.gms.common.data.DataHolder
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataItemAsset
import com.google.android.gms.wearable.DataItemBuffer
import com.google.android.gms.wearable.PutDataRequest
import java.util.concurrent.CopyOnWriteArrayList
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdbDataClient @Inject constructor(
    @ApplicationContext context: Context
) : DataClient(context, GoogleApi.Settings.Builder().build()) {

    private val listeners = CopyOnWriteArrayList<DataClient.OnDataChangedListener>()

    // Debug-only: empty DataItemBuffer with an empty DataHolder (0 rows).
    // Callers are not expected to iterate or release this buffer.
    // This is acceptable for debug builds — the real DataClient is used in release.
    @Suppress("DataBufferLeak")
    private val emptyDataItemBuffer = DataItemBuffer(DataHolder.builder(arrayOf("_id")).build(0))

    override fun addListener(listener: DataClient.OnDataChangedListener): Task<Void> {
        listeners.add(listener); return Tasks.forResult(null)
    }

    override fun removeListener(listener: DataClient.OnDataChangedListener): Task<Boolean> {
        listeners.remove(listener); return Tasks.forResult(true)
    }

    override fun addListener(
        listener: DataClient.OnDataChangedListener,
        uri: Uri,
        strategy: Int
    ): Task<Void> = addListener(listener)

    override fun getDataItem(uri: Uri): Task<DataItem?> = Tasks.forResult(null)

    override fun getDataItems(): Task<DataItemBuffer> = Tasks.forResult(emptyDataItemBuffer)

    override fun getDataItems(uri: Uri): Task<DataItemBuffer> = Tasks.forResult(emptyDataItemBuffer)

    override fun getDataItems(uri: Uri, filterType: Int): Task<DataItemBuffer> =
        Tasks.forResult(emptyDataItemBuffer)

    override fun putDataItem(request: PutDataRequest): Task<DataItem?> = Tasks.forResult(null)

    override fun deleteDataItems(uri: Uri): Task<Int> = Tasks.forResult(0)

    override fun deleteDataItems(uri: Uri, filterType: Int): Task<Int> = Tasks.forResult(0)

    override fun getFdForAsset(asset: Asset): Task<DataClient.GetFdForAssetResponse> =
        Tasks.forException(UnsupportedOperationException("getFdForAsset not supported in ADB bridge"))

    override fun getFdForAsset(asset: DataItemAsset): Task<DataClient.GetFdForAssetResponse> =
        Tasks.forException(UnsupportedOperationException("getFdForAsset not supported in ADB bridge"))
}
