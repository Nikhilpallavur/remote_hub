plugins {
    id("remotehub.android.library")
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.kapt)
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:drivers"))
    implementation(project(":core:common"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.core.ktx)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
}
