plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val appName = providers.gradleProperty("app.name").get()
val appId = providers.gradleProperty("app.id").get()
val appAbis = providers.gradleProperty("app.abis").get().split(',').map { it.trim() }.filter { it.isNotEmpty() }

android {
    namespace = "ytslim"
    compileSdk = 36

    defaultConfig {
        applicationId = appId
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        resValue("string", "app_name", appName)
        multiDexEnabled = true
        ndk { abiFilters += appAbis }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // O youtubeapi tem flavors (stbeta, ststable, stfdroid). O ststable usa o j2v8 local.
    flavorDimensions += "default"
    productFlavors {
        create("app") {
            dimension = "default"
            missingDimensionStrategy("default", "ststable")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            resValue("string", "app_name", "$appName (debug)")
        }
        release {
            isMinifyEnabled = false
            // Uso pessoal: assina com a chave de debug para instalar direto, sem keystore.
            signingConfig = signingConfigs.getByName("debug")
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
        resources.excludes += setOf(
            "META-INF/{AL2.0,LGPL2.1}",
            "META-INF/DEPENDENCIES",
            "META-INF/LICENSE*",
            "META-INF/NOTICE*",
            "META-INF/*.kotlin_module",
            "META-INF/versions/9/previous-compilation-data.bin",
        )
        jniLibs.useLegacyPackaging = true
    }

    testOptions.unitTests.isReturnDefaultValues = true
    testOptions.unitTests.isIncludeAndroidResources = true
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // Motor
    implementation(project(":mediaserviceinterfaces"))
    implementation(project(":youtubeapi"))
    implementation(project(":sharedutils"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.tv.material)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.dash)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.ui)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.ktor)
    implementation(libs.ktor.client.android)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.rx2)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}

// Os testes do motor falam com o YouTube de verdade; só rodam com -Pytslim.online=true.
val online = providers.gradleProperty("ytslim.online").map { it.toBoolean() }.getOrElse(false)
tasks.withType<Test>().configureEach {
    systemProperty("ytslim.online", online.toString())
    if (!online) filter.excludeTestsMatching("*OnlineTest")
    // Espelho opcional para o Robolectric baixar o android-all (ex.: quando o Maven Central limita).
    providers.gradleProperty("robolectric.repo").orNull?.let { systemProperty("robolectric.dependency.repo.url", it) }
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
