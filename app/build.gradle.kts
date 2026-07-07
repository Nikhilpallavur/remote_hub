plugins {
    id("remotehub.android.application")
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.kapt)
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:drivers"))
    implementation(project(":core:transport"))
    implementation(project(":core:data"))

    implementation(project(":feature:remote"))

    // Driver modules are on the app classpath so Hilt aggregates their @IntoSet
    // DeviceDriver bindings into the DriverRegistry. Adding a new device = add its
    // module + one line here; no existing code changes (Open/Closed).
    implementation(project(":device:tv"))
    implementation(project(":device:ir"))
    implementation(project(":device:ac"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(libs.coil.compose)
    implementation(libs.timber)

    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    kapt(libs.hilt.compiler)
}
