plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val appName = providers.gradleProperty("app.name").get()
val appId = providers.gradleProperty("app.id").get()
val appAbis = providers.gradleProperty("app.abis").get().split(',').map { it.trim() }.filter { it.isNotEmpty() }
val appVersionCode = providers.gradleProperty("app.versionCode").get().toInt()
val appVersionName = providers.gradleProperty("app.versionName").get()

// Chave das releases. Precisa ser sempre a mesma, senão o Android recusa a atualização.
// Local: -Pdoorglow.keystore=/caminho/doorglow.keystore. No GitHub Actions: DOORGLOW_KEYSTORE.
val releaseKeystore: String? = providers.gradleProperty("doorglow.keystore").orNull
    ?: providers.environmentVariable("DOORGLOW_KEYSTORE").orNull
fun signingValue(name: String, default: String): String =
    providers.gradleProperty("doorglow.$name").orNull
        ?: providers.environmentVariable("DOORGLOW_${name.uppercase().replace('.', '_')}").orNull
        ?: default

android {
    namespace = "ytslim"
    compileSdk = 36

    defaultConfig {
        applicationId = appId
        minSdk = 23
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName

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

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                // A chave atual foi gerada como chave de debug; por isso estes padrões.
                storePassword = signingValue("store.password", "android")
                keyAlias = signingValue("key.alias", "androiddebugkey")
                keyPassword = signingValue("key.password", "android")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            resValue("string", "app_name", "$appName (debug)")
        }
        release {
            // R8: otimiza o Compose, o Media3 e o resto, e tira o que não é usado (ex.: os milhares de
            // ícones do material-icons-extended). O motor fica inteiro (proguard-rules.pro).
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Sem a chave das releases (ex.: build local de teste), assina com a de debug desta máquina:
            // instala num aparelho limpo, mas não atualiza por cima da versão das releases.
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
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
    // Chamadas ao YouTube que o motor não tem (mover vídeo na playlist), pelo cliente HTTP dele.
    implementation(libs.okhttp.engine)
    // A busca com filtros combinados usa a API de busca do motor direto (EngineSearch.java).
    compileOnly(libs.retrofit.engine)

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
    // Tocar com a tela desligada: o serviço de mídia, a notificação e os controles da tela de bloqueio.
    implementation(libs.media3.session)

    // QR code do login na TV.
    implementation(libs.zxing.core)

    // Vidro fosco nas barras do celular e do tablet (no Android 12 ou mais novo; antes, cor sólida).
    implementation(libs.haze)

    implementation(libs.coil.compose)
    // Miniaturas pelo OkHttp 3 do motor, em HTTP/2 (ui/ImageHttp.kt).
    implementation(libs.coil.network.core)

    // Diz no log se o Android já compilou o app com o perfil (src/main/baseline-prof.txt).
    implementation(libs.androidx.profileinstaller)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.rx2)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    // Fotos das telas (ScreensShots): só com -Pytslim.screenshots=true.
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}

// Os testes do motor falam com o YouTube de verdade; só rodam com -Pytslim.online=true.
val online = providers.gradleProperty("ytslim.online").map { it.toBoolean() }.getOrElse(false)
// As fotos das telas (para comparar o visual) só rodam com -Pytslim.screenshots=<pasta das imagens de exemplo>.
val screenshots = providers.gradleProperty("ytslim.screenshots").orNull
tasks.withType<Test>().configureEach {
    systemProperty("ytslim.online", online.toString())
    if (!online) filter.excludeTestsMatching("*OnlineTest")
    if (screenshots == null) filter.excludeTestsMatching("*ScreenShots") else systemProperty("ytslim.screenshots", screenshots)
    // Espelho opcional para o Robolectric baixar o android-all (ex.: quando o Maven Central limita).
    providers.gradleProperty("robolectric.repo").orNull?.let { systemProperty("robolectric.dependency.repo.url", it) }
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
