plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "app.sipsure"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.sipsure"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // FileProvider hands the system camera a private file to write the check-in photo into.
    implementation("androidx.core:core:1.16.0")
    testImplementation("junit:junit:4.13.2")
    // Android's org.json is stubbed in JVM unit tests; use the real implementation there.
    testImplementation("org.json:json:20240303")
}
