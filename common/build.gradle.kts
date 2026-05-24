plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.detekt)
}

detekt {
    toolVersion = libs.versions.detekt.get()
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    allRules = false
    basePath = rootProject.projectDir
    parallel = true
    source.setFrom("src/main/kotlin", "src/main/java")
    baseline.set(file("detekt-baseline-main.xml"))
}

tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
    jvmTarget = "21"
    reports {
        html.required.set(true)
        sarif.required.set(false)
        checkstyle.required.set(false)
        markdown.required.set(false)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // Compose rules (harmless on pure JVM, kept for consistency across modules)
    detektPlugins(libs.detekt.compose.rules)
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.datetime)
    api(libs.dagger)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
