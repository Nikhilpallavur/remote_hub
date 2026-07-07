plugins {
    id("remotehub.android.library")
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.kapt)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.timber)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
}
