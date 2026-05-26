package pl.dekrate.kofeino.tracker.data.sync

import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Overridable for tests — set to a [TestDispatcher] to avoid
 * suspending on real IO threads.
 *
 * WARNING: Top-level mutable state. Only reassign during test setup.
 * Not thread-safe — do not modify from multiple threads concurrently.
 */
internal var ioCoroutineDispatcher: CoroutineDispatcher = Dispatchers.IO

/**
 * Coroutine-friendly [com.google.android.gms.tasks.Task] bridge.
 *
 * Wraps the blocking [Tasks.await] call inside [Dispatchers.IO] so it can
 * be safely used from any coroutine context without blocking the caller's thread.
 */
suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T =
    withContext(ioCoroutineDispatcher) {
        Tasks.await(this@await)
    }
