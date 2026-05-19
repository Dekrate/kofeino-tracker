plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
}

val detektAll by tasks.registering {
    description = "Runs detektMain for all subprojects"
    subprojects.forEach { dependsOn("${it.path}:detektMain") }
}

val detektBaselineAll by tasks.registering {
    description = "Generates detekt baselines for all subprojects (main sources)"
    subprojects.forEach { dependsOn("${it.path}:detektBaselineMain") }
}
