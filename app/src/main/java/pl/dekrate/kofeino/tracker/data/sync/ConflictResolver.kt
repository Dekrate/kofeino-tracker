package pl.dekrate.kofeino.tracker.data.sync

import timber.log.Timber
import javax.inject.Inject

/**
 * Pure conflict resolution engine using Last-Write-Wins (LWW) strategy.
 *
 * Stateless and deterministic — same inputs always produce the same output.
 * Designed as a pure function so it is trivially testable without mocking.
 *
 * ## Resolution rules
 * 1. **Delete wins** — a DELETE operation overrides any INSERT/UPDATE.
 * 2. **Newer timestamp wins** — the change with the later timestamp is applied.
 * 3. **Equal timestamp → local device wins** — when timestamps match, the local
 *    device takes precedence (phone-first bias).
 * 4. **Same timestamp + same source → idempotent** — identical data requires
 *    no action.
 * 5. **Clock skew (≥ 60 s)** — timestamps are still used for comparison;
 *    a warning is logged so operators can investigate.
 */
class ConflictResolver @Inject constructor() {

    /**
     * Resolve a single conflict between the local entity state and an incoming sync change.
     *
     * @param local    Current state of the entity on this device, or `null` if the
     *                 entity does not exist locally yet.
     * @param incoming The change received from the paired device.
     * @return A [ConflictResolution] that describes the action the caller should take.
     */
    fun resolve(
        local: SyncChange?,
        incoming: SyncChange
    ): ConflictResolution {
        // ── No local data → unconditionally accept incoming ──────────────
        if (local == null) {
            return ConflictResolution.AcceptIncoming(incoming)
        }

        // ── Delete always wins (vs non-delete only) ──────────────────────
        // When one side is DELETE and the other is INSERT/UPDATE, the delete wins.
        // When both are DELETE (or both non-delete) we fall through to LWW.
        val incomingDelete = incoming.operationType == PendingChangeEntity.OPERATION_DELETE
        val localDelete = local.operationType == PendingChangeEntity.OPERATION_DELETE
        if (incomingDelete != localDelete) {
            return if (incomingDelete) {
                ConflictResolution.AcceptIncoming(incoming)
            } else {
                ConflictResolution.KeepLocal(local)
            }
        }

        // ── Same content → idempotent ────────────────────────────────────
        if (local.timestamp == incoming.timestamp && local.source == incoming.source) {
            return ConflictResolution.NoOp(local)
        }

        // ── Clock skew detection ─────────────────────────────────────────
        val skewMs = kotlin.math.abs(local.timestamp - incoming.timestamp)
        if (skewMs >= CLOCK_SKEW_THRESHOLD_MS) {
            Timber.w(
                "Clock skew detected: %d ms between local=%d and incoming=%d",
                skewMs, local.timestamp, incoming.timestamp
            )
        }

        // ── Last-write-wins: newer timestamp wins ────────────────────────
        return when {
            incoming.timestamp > local.timestamp -> ConflictResolution.AcceptIncoming(incoming)
            local.timestamp > incoming.timestamp -> ConflictResolution.KeepLocal(local)
            // Equal timestamp → local device wins
            else -> ConflictResolution.KeepLocal(local)
        }
    }

    /**
     * Resolve a batch of incoming changes against the current local state.
     *
     * This is equivalent to calling [resolve] for each incoming change
     * individually, but is more efficient because it builds a lookup map
     * from the local list only once.
     *
     * @param local    All current local entities relevant to the batch.
     * @param incoming All incoming changes from the paired device.
     * @return One [ConflictResolution] per incoming change, in the same order.
     */
    fun resolveBatch(
        local: List<SyncChange>,
        incoming: List<SyncChange>
    ): List<ConflictResolution> {
        val localMap = local.associateBy { it.entityId to it.entityType }
        return incoming.map { change ->
            resolve(localMap[change.entityId to change.entityType], change)
        }
    }

    companion object {
        /**
         * Threshold above which a timestamp difference is logged as a clock-skew
         * warning (in milliseconds). Default: 60 seconds.
         */
        const val CLOCK_SKEW_THRESHOLD_MS = 60_000L
    }
}

/**
 * Lightweight representation of a sync change used by [ConflictResolver].
 *
 * Mirrors the fields of [PendingChangeEntity] but adds a [source] discriminator
 * so the resolver can tell which device originated the change.
 */
data class SyncChange(
    val entityType: String,
    val entityId: String,
    val operationType: String,
    val payload: String,
    val timestamp: Long,
    val source: DeviceSource
)

/**
 * Identifies the device that originated a sync change.
 */
enum class DeviceSource {
    PHONE,
    WATCH
}

/**
 * Immutable result of a conflict resolution.
 *
 * The caller is expected to pattern-match on this sealed class and perform
 * the corresponding database / sync action.
 */
sealed class ConflictResolution {
    /** The local version should be kept; the incoming change is discarded. */
    data class KeepLocal(val change: SyncChange) : ConflictResolution()

    /** The incoming change should be applied over the local state. */
    data class AcceptIncoming(val change: SyncChange) : ConflictResolution()

    /** No action required — both sides carry identical data. */
    data class NoOp(val local: SyncChange) : ConflictResolution()
}
