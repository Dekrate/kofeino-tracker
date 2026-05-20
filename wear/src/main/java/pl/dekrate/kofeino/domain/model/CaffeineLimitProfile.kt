package pl.dekrate.kofeino.domain.model

import pl.dekrate.kofeino.R

/**
 * Profiles for configurable daily caffeine limit, based on authoritative health guidelines:
 *
 * - **EFSA (2015)** — Scientific Opinion on the safety of caffeine.
 *   Single doses up to 200 mg and habitual intake up to **400 mg/day** do not raise safety
 *   concerns for non-pregnant adults. For pregnant women, up to **200 mg/day** is safe.
 *   (https://efsa.europa.eu/en/efsajournal/pub/4102)
 *
 * - **FDA (2024)** — 400 mg/day is safe for most adults.
 *   (https://www.fda.gov/consumers/consumer-updates/spilling-beans-how-much-caffeine-too-much)
 *
 * - **WHO (2023)** — Pregnant women with >300 mg/day should lower intake.
 *   (https://www.who.int/tools/elena/interventions/caffeine-pregnancy)
 *
 * - **Mayo Clinic (2022)** — Up to 400 mg/day is safe for most adults.
 *   (https://www.mayoclinic.org/healthy-lifestyle/nutrition-and-healthy-eating/in-depth/caffeine/art-20045678)
 */
enum class CaffeineLimitProfile(
    val displayNameResId: Int,
    val descriptionResId: Int,
    /** The maximum daily limit in mg, or `null` for custom (user-defined). */
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
