import com.android.build.api.dsl.ApplicationExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

pluginManager.apply("org.jetbrains.kotlin.android")

extensions.configure<ApplicationExtension>("android") {
    namespace = "com.nikhilpallavur.remotehub"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nikhilpallavur.remotehub"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            // Store-ready release: shrink, optimize and obfuscate with R8.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            // BouncyCastle's bcpkix/bcutil/bcprov jars each ship an identical
            // multi-release OSGi manifest; drop the duplicates so packaging succeeds.
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
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
