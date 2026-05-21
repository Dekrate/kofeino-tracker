package pl.dekrate.kofeino.data.sync

import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Coroutine-friendly [com.google.android.gms.tasks.Task] bridge.
 *
 * ⚠ Mirror of the phone module — keep in sync.
 */
suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T =
    withContext(Dispatchers.IO) {
        Tasks.await(this@await)
    }
