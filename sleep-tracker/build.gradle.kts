// Top-level build file. Plugins are declared here (not applied) so both modules share one version.
// Kotlin support is built into the Android Gradle Plugin (AGP 9+), so there is no kotlin-android plugin.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
