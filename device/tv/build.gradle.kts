plugins {
    id("remotehub.android.library")
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.kapt)
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:drivers"))
    implementation(project(":core:transport"))
    implementation(project(":core:common"))
    implementation(libs.okhttp)
    // Samsung/LG protocols build and parse SSAP/WebSocket JSON payloads at runtime.
    implementation(libs.kotlinx.serialization.json)
    // Mints the self-signed RSA client certificate the Android TV Remote v2 pairing
    // handshake pins — the platform JSSE cannot generate one on its own.
    implementation(libs.bouncycastle.bcpkix)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.core.ktx)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
}
