import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val testAdMobAppId = "ca-app-pub-3940256099942544~3347511713"
val testBannerAdUnitId = "ca-app-pub-3940256099942544/9214589741"
val releaseAdMobAppId = providers.gradleProperty("admobAppId").orNull
val releaseBannerAdUnitId = providers.gradleProperty("admobBannerAdUnitId").orNull

android {
    namespace = "com.pedometer.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pedometer.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
        manifestPlaceholders["adMobAppId"] = testAdMobAppId
        buildConfigField("String", "ADMOB_BANNER_AD_UNIT_ID", "\"$testBannerAdUnitId\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            manifestPlaceholders["adMobAppId"] = releaseAdMobAppId ?: testAdMobAppId
            buildConfigField(
                "String",
                "ADMOB_BANNER_AD_UNIT_ID",
                "\"${releaseBannerAdUnitId ?: testBannerAdUnitId}\""
            )
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


    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Export the Room schema to /schemas so migrations can be reviewed in PRs.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

// KGP 2.0 style: top-level kotlin extension (NOT inside the android block).
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)

    // Lifecycle + ViewModel (Compose integration)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Compose (BOM manages versions)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    // Activity for edge-to-edge + permission contracts
    implementation(libs.androidx.activity.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // DataStore (user preferences)
    implementation(libs.datastore.preferences)

    // Monetization
    implementation(libs.billing.client)
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
}

tasks.configureEach {
    if (name.contains("Release", ignoreCase = true)) {
        doFirst {
            check(!releaseAdMobAppId.isNullOrBlank()) {
                "Set -PadmobAppId to your AdMob app ID before building a release."
            }
            check(!releaseBannerAdUnitId.isNullOrBlank()) {
                "Set -PadmobBannerAdUnitId to your production banner ad unit ID before building a release."
            }
        }
    }
}
