package pl.dekrate.kofeino.tracker.data.backup

/**
 * JSON schema for Backup/Restore export files.
 *
 * The schema is forward-compatible via the [version] field. Every consumer
 * MUST check the version before deserialising and reject unknown versions.
 *
 * ## Schema history
 * - **v1** – initial release: intakes + custom drinks + settings
 */
data class BackupData(
    /** Schema version — used for forward-compat checks in import. */
    val version: Int = CURRENT_VERSION,

    /** ISO-8601 timestamp of when the backup was created. */
    val exportedAt: String,

    /** All tracked caffeine intakes at time of export. */
    val intakes: List<BackupIntake> = emptyList(),

    /** User's custom (and default) drinks at time of export. */
    val drinks: List<BackupDrink> = emptyList(),

    /** App settings snapshot. */
    val settings: BackupSettings = BackupSettings()
) {
    companion object {
        const val CURRENT_VERSION = 1
        const val MIN_SUPPORTED_VERSION = 1
    }
}

/** Serializable representation of a single [pl.dekrate.kofeino.tracker.domain.model.CaffeineIntake]. */
data class BackupIntake(
    val id: Long = 0,
    val drinkId: Long? = null,
    val drinkName: String,
    val caffeineMg: Int,
    val volumeMl: Int,
    val timestamp: Long
)

/** Serializable representation of a single [pl.dekrate.kofeino.tracker.domain.model.DrinkEntity]. */
data class BackupDrink(
    val id: Long = 0,
    val name: String,
    val caffeineMg: Int,
    val volumeMl: Int,
    val isDefault: Boolean = false
)

/** Serializable snapshot of user-facing app settings. */
data class BackupSettings(
    val dailyLimitMg: Int = 400,
    val language: String = "system",
    val themeMode: String = "system",
    val notifLiveEnabled: Boolean = true,
    val notifMorningEnabled: Boolean = false,
    val notifRegularEnabled: Boolean = false,
    val notifEveningEnabled: Boolean = false
)
