import com.android.build.api.dsl.LibraryExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
}

pluginManager.apply("org.jetbrains.kotlin.android")

extensions.configure<LibraryExtension>("android") {
    namespace = project.path.toRemoteHubNamespace()
    compileSdk = 35
    sourceSets.named("main") {
        manifest.srcFile(rootProject.file("config/android/empty-android-manifest.xml"))
    }

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
        buildConfig = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = true
        warningsAsErrors = false
        checkDependencies = true
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    "testImplementation"("junit:junit:4.13.2")
    "testImplementation"("org.jetbrains.kotlin:kotlin-test:2.0.0")
    "testImplementation"("com.google.truth:truth:1.4.4")
    "testImplementation"("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    "testImplementation"("io.mockk:mockk:1.13.12")
}

fun String.toRemoteHubNamespace(): String {
    return "com.nikhilpallavur.remotehub" + replace(":", ".")
        .replace("-", "")
}
