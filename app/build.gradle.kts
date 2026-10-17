plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.resumebuilder"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.dineshdikshit.resumebuilder"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    // Scrolling lists for resumes and section entries.
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    // Periodic on-device backup job (works offline, has a Java API).
    implementation("androidx.work:work-runtime:2.10.1")
    // Asks the Play Store app whether a newer version exists (Java API, no internet permission needed).
    implementation("com.google.android.play:app-update:2.1.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}