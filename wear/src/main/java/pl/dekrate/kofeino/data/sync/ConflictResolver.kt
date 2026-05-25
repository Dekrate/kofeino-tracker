package pl.dekrate.kofeino.data.sync

import com.google.gson.Gson
import pl.dekrate.kofeino.domain.model.CaffeineIntake
import pl.dekrate.kofeino.domain.model.DrinkEntity

/**
 * Last-Write-Wins conflict resolver with timestamp + source device tiebreaker.
 *
 * Rules:
 * 1. Delete always wins over update/insert of the same entity
 * 2. Compare timestamps: newer wins
 * 3. If timestamps are equal (within 1ms): phone wins ("phone" sourceDeviceId)
 * 4. Loser data is returned for logging, not lost
 *
 * Clock skew: if devices differ by more than 60s, a warning is logged
 * but timestamp is still used as-is.
 */
object ConflictResolver {

    private val gson = Gson()
    private const val CLOCK_SKEW_THRESHOLD_MS = 60_000L
    private const val TIMESTAMP_TOLERANCE_MS = 1L

    const val PHONE_DEVICE_ID = "phone"
    const val WATCH_DEVICE_ID = "watch"

    const val REASON_LOCAL_NEWER = "local_timestamp_newer"
    const val REASON_INCOMING_NEWER = "incoming_timestamp_newer"
    const val REASON_PHONE_WINS_TIE = "phone_wins_tiebreaker"
    const val REASON_LOCAL_PHONE_WINS = "local_phone_wins_tiebreaker"
    const val REASON_DELETE_WINS = "delete_wins"
    const val REASON_EQUAL_NO_CONFLICT = "no_conflict_equal"

    data class ConflictResult<T>(
        val winner: T,
        val reason: String,
        val clockSkewWarning: String? = null,
        val wasConflict: Boolean = false
    )

    data class TimestampComparison(
        val reason: String,
        val localWins: Boolean,
        val clockSkewWarning: String?
    )

    /**
     * Resolve conflict between a local and incoming [CaffeineIntake].
     *
     * @param local the entity currently stored locally
     * @param incoming the entity received from the paired device
     * @param operationType the incoming operation type (INSERT, UPDATE, DELETE)
     * @return [ConflictResult] with the winning entity and decision reason
     */
    fun resolveIntakeConflict(
        local: CaffeineIntake?,
        incoming: CaffeineIntake,
        operationType: String
    ): ConflictResult<CaffeineIntake> {
        if (operationType == PendingChangeEntity.OPERATION_DELETE) {
            return ConflictResult(
                winner = incoming,
                reason = REASON_DELETE_WINS,
                wasConflict = local != null
            )
        }

        if (local == null) {
            return ConflictResult(
                winner = incoming,
                reason = REASON_EQUAL_NO_CONFLICT,
                wasConflict = false
            )
        }

        val comparison = compareTimestamps(
            local.lastModifiedTimestamp, local.sourceDeviceId,
            incoming.lastModifiedTimestamp
        )

        return if (comparison.localWins) {
            ConflictResult(
                winner = local,
                reason = comparison.reason,
                clockSkewWarning = comparison.clockSkewWarning,
                wasConflict = true
            )
        } else {
            ConflictResult(
                winner = incoming,
                reason = comparison.reason,
                clockSkewWarning = comparison.clockSkewWarning,
                wasConflict = true
            )
        }
    }

    /**
     * Resolve conflict between a local and incoming [DrinkEntity].
     */
    fun resolveDrinkConflict(
        local: DrinkEntity?,
        incoming: DrinkEntity,
        operationType: String
    ): ConflictResult<DrinkEntity> {
        if (operationType == PendingChangeEntity.OPERATION_DELETE) {
            return ConflictResult(
                winner = incoming,
                reason = REASON_DELETE_WINS,
                wasConflict = local != null
            )
        }

        if (local == null) {
            return ConflictResult(
                winner = incoming,
                reason = REASON_EQUAL_NO_CONFLICT,
                wasConflict = false
            )
        }

        val comparison = compareTimestamps(
            local.lastModifiedTimestamp, local.sourceDeviceId,
            incoming.lastModifiedTimestamp
        )

        return if (comparison.localWins) {
            ConflictResult(
                winner = local,
                reason = comparison.reason,
                clockSkewWarning = comparison.clockSkewWarning,
                wasConflict = true
            )
        } else {
            ConflictResult(
                winner = incoming,
                reason = comparison.reason,
                clockSkewWarning = comparison.clockSkewWarning,
                wasConflict = true
            )
        }
    }

    /**
     * Compare two timestamps with LWW + tiebreaker logic.
     *
     * @return [TimestampComparison] indicating which side wins and why.
     */
    fun compareTimestamps(
        localTimestamp: Long,
        localSourceId: String,
        incomingTimestamp: Long
    ): TimestampComparison {
        val timeDiff = incomingTimestamp - localTimestamp
        val absDiff = kotlin.math.abs(timeDiff)

        val clockSkewWarning = if (absDiff > CLOCK_SKEW_THRESHOLD_MS) {
            "Clock skew detected: devices differ by ${absDiff}ms (>${CLOCK_SKEW_THRESHOLD_MS}ms). Using timestamp as-is."
        } else null

        return when {
            timeDiff < -TIMESTAMP_TOLERANCE_MS -> TimestampComparison(
                reason = REASON_LOCAL_NEWER,
                localWins = true,
                clockSkewWarning = clockSkewWarning
            )
            timeDiff > TIMESTAMP_TOLERANCE_MS -> TimestampComparison(
                reason = REASON_INCOMING_NEWER,
                localWins = false,
                clockSkewWarning = clockSkewWarning
            )
            else -> {
                if (localSourceId == PHONE_DEVICE_ID) {
                    TimestampComparison(
                        reason = REASON_LOCAL_PHONE_WINS,
                        localWins = true,
                        clockSkewWarning = clockSkewWarning
                    )
                } else {
                    TimestampComparison(
                        reason = REASON_PHONE_WINS_TIE,
                        localWins = false,
                        clockSkewWarning = clockSkewWarning
                    )
                }
            }
        }
    }

    /**
     * Serialize any entity to JSON for conflict logging.
     */
    fun serializeEntity(entity: Any?): String {
        return if (entity != null) gson.toJson(entity) else "null"
    }
}
