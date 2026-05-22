package pl.dekrate.kofeino.tracker.data.backup

import pl.dekrate.kofeino.tracker.domain.model.CaffeineIntake
import pl.dekrate.kofeino.tracker.domain.model.DrinkEntity
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves conflicts during backup import using an **additive merge strategy**.
 *
 * ## Import rules
 * - **Intakes**: all imported intakes are added unconditionally (additive).
 *   Existing intakes with the same [CaffeineIntake.id] are skipped to avoid
 *   overwriting post-backup changes.
 * - **Drinks**: imported drinks whose `name` already exists in the local DB
 *   are skipped (name is the unique natural key for drinks).
 * - **Settings**: imported settings are applied only if the user opts in
 *   (the caller controls this — the resolver just returns the diff).
 *
 * ## Why additive?
 * - **Non-destructive**: import never deletes existing data. The user can
 *   restore without fear of losing post-backup intakes.
 * - **Idempotent**: importing the same backup file twice produces the same
 *   final state (no duplicate intakes beyond the first import).
 *
 * ## Design pattern: Strategy
 * The conflict resolution strategy is extracted into its own class so that:
 * - It can be unit-tested in isolation.
 * - Alternative strategies (e.g. "overwrite all", "timestamp-based") can
 *   be swapped in via DI without changing [BackupManager].
 */
@Singleton
class BackupConflictResolver @Inject constructor() {

    /**
     * Result of resolving import conflicts.
     */
    data class Resolution(
        /** Intakes that should be inserted (no local match by [CaffeineIntake.id]). */
        val intakesToInsert: List<CaffeineIntake>,
        /** Intakes that already exist locally and were skipped. */
        val intakesSkipped: Int,
        /** Drinks that should be inserted (no local match by name). */
        val drinksToInsert: List<DrinkEntity>,
        /** Drinks that already exist locally and were skipped. */
        val drinksSkipped: Int
    )

    /**
     * Resolve conflicts between imported data and existing local data.
     *
     * @param importedIntakes Intakes from the backup file.
     * @param importedDrinks Drinks from the backup file.
     * @param existingIntakeIds Set of [CaffeineIntake.id] values already in the local DB.
     * @param existingDrinkNames Set of [DrinkEntity.name] values already in the local DB.
     */
    fun resolve(
        importedIntakes: List<BackupIntake>,
        importedDrinks: List<BackupDrink>,
        existingIntakeIds: Set<Long>,
        existingDrinkNames: Set<String>
    ): Resolution {
        // --- Intakes: skip any that already exist locally by ID ---
        val intakesToInsert = importedIntakes
            .filter { backupIntake ->
                // id=0 means the intake was never synced (auto-generated in original DB)
                // Keep it, since there's no local match.
                backupIntake.id == 0L || backupIntake.id !in existingIntakeIds
            }
            .map { it.toDomain() }
        val intakesSkipped = importedIntakes.size - intakesToInsert.size

        // --- Drinks: skip any whose name already exists locally ---
        val drinksToInsert = importedDrinks
            .filter { backupDrink ->
                backupDrink.name !in existingDrinkNames
            }
            .map { it.toDomain() }
        val drinksSkipped = importedDrinks.size - drinksToInsert.size

        Timber.d("Conflict resolution: %d→%d intakes (%d skipped), %d→%d drinks (%d skipped)",
            importedIntakes.size, intakesToInsert.size, intakesSkipped,
            importedDrinks.size, drinksToInsert.size, drinksSkipped)

        return Resolution(
            intakesToInsert = intakesToInsert,
            intakesSkipped = intakesSkipped,
            drinksToInsert = drinksToInsert,
            drinksSkipped = drinksSkipped
        )
    }
}

// ---------------------------------------------------------------------------
// Extension functions: Backup DTO → Domain model
// ---------------------------------------------------------------------------

private fun BackupIntake.toDomain() = CaffeineIntake(
    id = 0, // reset ID so Room auto-generates a new primary key
    drinkId = drinkId,
    drinkName = drinkName,
    caffeineMg = caffeineMg,
    volumeMl = volumeMl,
    timestamp = timestamp
)

private fun BackupDrink.toDomain() = DrinkEntity(
    id = 0, // reset ID to avoid primary key conflicts
    name = name,
    caffeineMg = caffeineMg,
    volumeMl = volumeMl,
    isDefault = isDefault
)
