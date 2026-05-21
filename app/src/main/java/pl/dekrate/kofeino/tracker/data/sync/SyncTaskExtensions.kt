package pl.dekrate.kofeino.tracker.data.sync

import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Coroutine-friendly [com.google.android.gms.tasks.Task] bridge.
 *
 * Wraps the blocking [Tasks.await] call inside [Dispatchers.IO] so it can
 * be safely used from any coroutine context without blocking the caller's thread.
 */
suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T =
    withContext(Dispatchers.IO) {
        Tasks.await(this@await)
    }
