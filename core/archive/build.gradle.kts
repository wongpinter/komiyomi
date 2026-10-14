plugins {
    id("mihon.library")
    kotlin("android")
    kotlin("plugin.serialization")
}

android {
    namespace = "mihon.core.archive"
}

dependencies {
    implementation(libs.jsoup)
    implementation(libs.libarchive)
    implementation(libs.unifile)
    // KMK -->
    implementation(projects.core.common)
    implementation(libs.injekt)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    // KMK <--
}
