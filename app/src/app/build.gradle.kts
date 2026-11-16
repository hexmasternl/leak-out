import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

// CI passes -PversionCode=<n> so every Play upload has a higher code; local builds keep the default.
val appVersionCode: Int = providers.gradleProperty("versionCode").orNull?.let { value ->
    value.toIntOrNull()?.takeIf { it > 0 }
        ?: throw GradleException("versionCode must be a positive integer, but was '$value'.")
} ?: 1

// CI passes -PversionName=<x.y.z> computed by GitVersion (see docs/deployment.md#versioning).
val appVersionName: String = providers.gradleProperty("versionName").orNull?.let { value ->
    value.takeIf { it.matches(Regex("""\d+\.\d+\.\d+""")) }
        ?: throw GradleException("versionName must look like <major>.<minor>.<patch>, but was '$value'.")
} ?: "0.1.0"

// Release signing with the Play upload key, configured only when CI provides it (see docs/deployment.md).
// Without LEAKOUT_UPLOAD_STORE_FILE the release build stays unsigned, so no keystore is needed locally.
val uploadStoreFile: String? = providers.environmentVariable("LEAKOUT_UPLOAD_STORE_FILE").orNull
fun requiredSigningEnv(name: String): String =
    providers.environmentVariable(name).orNull?.takeIf { it.isNotEmpty() }
        ?: throw GradleException("LEAKOUT_UPLOAD_STORE_FILE is set, but $name is missing.")

// The Leak Out score server (openspec/specs/online-score-submission). Both values are public, so the
// defaults live here; -Pleakout.scoreServerUrl / LEAKOUT_SCORE_SERVER_URL (and the client id pair)
// override them, and a blank value turns score posting off. The URL is the live Container App's own
// host; change it here when the server gets a custom domain.
fun scoreServerSetting(property: String, env: String, default: String): String =
    (providers.gradleProperty(property).orNull ?: providers.environmentVariable(env).orNull ?: default).trim()
val scoreServerUrl = scoreServerSetting("leakout.scoreServerUrl", "LEAKOUT_SCORE_SERVER_URL", "https://rg-leakout-prod-neu-api.jollywater-e41c70f8.northeurope.azurecontainerapps.io")
val scoreServerClientId = scoreServerSetting(
    "leakout.scoreServerClientId",
    "LEAKOUT_SCORE_SERVER_CLIENT_ID",
    "201820010117-uteqrtu77v48c9ovd8d9k9qan0sktcj4.apps.googleusercontent.com",
)

// AdMob (openspec/specs/session-start-ads). Debug builds always use Google's test ids, so developing never
// clicks live ads. Release builds use the Leak Out app id and "game-start-add" interstitial unit below. Both are
// public (they ship inside every APK, like the score server client id above); LEAKOUT_ADMOB_APP_ID and
// LEAKOUT_ADMOB_INTERSTITIAL_ID override them, and only as a pair because the two ids belong together.
val admobTestAppId = "ca-app-pub-3940256099942544~3347511713"
val admobTestInterstitialId = "ca-app-pub-3940256099942544/1033173712"
val admobLeakOutAppId = "ca-app-pub-1310013076234295~4675407791"
val admobLeakOutInterstitialId = "ca-app-pub-1310013076234295/8713231863"
fun admobEnv(name: String): String? = providers.environmentVariable(name).orNull?.trim()?.takeIf { it.isNotEmpty() }
val admobOverrideAppId = admobEnv("LEAKOUT_ADMOB_APP_ID")
val admobOverrideInterstitialId = admobEnv("LEAKOUT_ADMOB_INTERSTITIAL_ID")
val admobReleaseIds = if (admobOverrideAppId != null && admobOverrideInterstitialId != null) {
    admobOverrideAppId to admobOverrideInterstitialId
} else {
    admobLeakOutAppId to admobLeakOutInterstitialId
}

android {
    namespace = "nl.hexmaster.leakout"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "nl.hexmaster.leakout"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SCORE_SERVER_URL", "\"$scoreServerUrl\"")
        buildConfigField("String", "SCORE_SERVER_CLIENT_ID", "\"$scoreServerClientId\"")
    }

    signingConfigs {
        if (!uploadStoreFile.isNullOrEmpty()) {
            create("upload") {
                storeFile = file(uploadStoreFile)
                storePassword = requiredSigningEnv("LEAKOUT_UPLOAD_STORE_PASSWORD")
                keyAlias = requiredSigningEnv("LEAKOUT_UPLOAD_KEY_ALIAS")
                keyPassword = requiredSigningEnv("LEAKOUT_UPLOAD_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        // Feature flags, fixed per build type and read through FeatureFlags (see openspec/specs/feature-flags).
        debug {
            manifestPlaceholders["admobAppId"] = admobTestAppId
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"$admobTestInterstitialId\"")
            buildConfigField("boolean", "LEVEL_DESIGNER", "true")
            // en-XA (long, accented) and ar-XB (right to left) for checking translated layouts; PseudoLocaleLayoutTest uses en-XA.
            isPseudoLocalesEnabled = true
        }
        release {
            manifestPlaceholders["admobAppId"] = admobReleaseIds.first
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"${admobReleaseIds.second}\"")
            buildConfigField("boolean", "LEVEL_DESIGNER", "false")
            signingConfigs.findByName("upload")?.let { signingConfig = it }
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    // The exported schemas are test assets, so migration tests can open every past version.
    sourceSets.getByName("androidTest") {
        assets.directories.add("$projectDir/schemas")
    }

    // Android stubs return defaults in JVM tests, so Play's FakeAppUpdateManager can run there (PlayAppUpdatesTest).
    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Text reaches players in other languages only through strings.xml (openspec/specs/app-localization).
    // HardcodedText covers XML; HardcodedTextTest covers the Compose sources.
    lint {
        error += "HardcodedText"
    }
}

// Every schema version is exported and committed; a version bump needs a written migration.
room {
    schemaDirectory("$projectDir/schemas")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.jdk.get().toInt()))
    }
}

dependencies {
    implementation(project(":core"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(project(":core"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.compose)
    // SavedStateHandle: the level designer keeps its draft across process death.
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)

    // Reads the background music credits (assets/music/audio_credits.json).
    implementation(libs.kotlinx.serialization.json)

    // Room: the on-device level history.
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore (Preferences): the player's settings.
    implementation(libs.androidx.datastore.preferences)

    // Play Games Services v2: silent and manual sign-in, and pushing the local achievements to Play Games.
    implementation(libs.play.services.games.v2)
    // The app uses no fragments: this only overrides the outdated fragment 1.1.0 that Play services asks for (issue #88).
    implementation(libs.androidx.fragment)

    // Play In-App Updates: asks Play for a newer version and its priority (openspec/specs/app-updates).
    implementation(libs.play.app.update)
    implementation(libs.play.app.update.ktx)

    // AdMob: one interstitial at session start (openspec/specs/session-start-ads) and the UMP consent form it needs.
    implementation(libs.play.services.ads)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}


// AGP only creates unit tests for the debug build type by default. The release variant gets them
// too, so FeatureFlagsReleaseTest can check the release build's flags (:app:testReleaseUnitTest).
androidComponents {
    beforeVariants(selector().withBuildType("release")) { variant ->
        variant.hostTests[com.android.build.api.variant.HostTestBuilder.UNIT_TEST_TYPE]?.enable = true
    }
}
