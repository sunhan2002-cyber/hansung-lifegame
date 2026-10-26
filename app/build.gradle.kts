plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.lifegame"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.lifegame"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// Unit tests load the authored JSON and image routing directly from this directory.
// A content-only change must invalidate Gradle's cached test result too.
tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    inputs.dir("src/main/assets")
}

// Explicit long-run QA entry point; this is not a skipped or weakened unit test.
tasks.register<JavaExec>("week10Simulation") {
    group = "verification"
    description = "Run 10,000 real-content lives and write reproducible week 10 QA evidence"
    dependsOn("testDebugUnitTest")
    mainClass.set("com.example.lifegame.domain.engine.Week10SimulationRunner")
    classpath = files(provider {
        tasks.named<org.gradle.api.tasks.testing.Test>("testDebugUnitTest").get().classpath
    })
    workingDir = projectDir
    maxHeapSize = "1536m"
    args(
        providers.gradleProperty("week10ReportDir").getOrElse("build/reports/week10"),
        providers.gradleProperty("week10SourceCommit").getOrElse("unrecorded"),
    )
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.4")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    // Android 16 removed the reflective InputManager API used by Espresso 3.6.
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
