package pl.dekrate.kofeino.tracker.data.local

import pl.dekrate.kofeino.tracker.R

/**
 * Caffeine limit profiles matching the watch module's [pl.dekrate.kofeino.domain.model.CaffeineLimitProfile].
 *
 * Each profile has a display name, description, and a preset limit in mg.
 * [CUSTOM] has a null limit — the user provides their own via [CaffeinePreferences].
 */
enum class CaffeineLimitProfile(
    val displayNameResId: Int,
    val descriptionResId: Int,
    val limitMg: Int?
) {
    ADULT(
        displayNameResId = R.string.profile_adult,
        descriptionResId = R.string.profile_adult_desc,
        limitMg = 400
    ),
    PREGNANT(
        displayNameResId = R.string.profile_pregnant,
        descriptionResId = R.string.profile_pregnant_desc,
        limitMg = 200
    ),
    SENSITIVE(
        displayNameResId = R.string.profile_sensitive,
        descriptionResId = R.string.profile_sensitive_desc,
        limitMg = 100
    ),
    CUSTOM(
        displayNameResId = R.string.profile_custom,
        descriptionResId = R.string.profile_custom_desc,
        limitMg = null
    );
}
