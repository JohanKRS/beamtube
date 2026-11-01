import com.android.build.api.dsl.LibraryExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

// ---------------------------------------------------------------------------
// Adaptação dos módulos do motor (MediaServiceCore e SharedModules) ao AGP 8,
// feita daqui para não editar os submódulos.
// ---------------------------------------------------------------------------
val engineModules = setOf(
    "mediaserviceinterfaces", "youtubeapi",
    "sharedutils", "sharedtests", "appupdatechecker2", "commons-io-2.8.0", "j2v8",
)

subprojects {
    if (name !in engineModules) return@subprojects

    // O AGP 8 exige `namespace`; os módulos ainda usam package= no manifest.
    plugins.withId("com.android.library") {
        extensions.configure<LibraryExtension> {
            val manifest = file("src/main/AndroidManifest.xml")
            val pkg = Regex("""package\s*=\s*"([^"]+)"""").find(manifest.readText())?.groupValues?.get(1)
            namespace = pkg ?: "ytslim.engine.${name.replace('-', '_')}"
            lint { abortOnError = false }
        }
    }

    // Os módulos compilam Java 8; o Kotlin precisa do mesmo alvo.
    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_1_8)
    }
}

// Versões que o SmartTube força no build dele (o motor depende delas).
val engineForcedVersions = listOf(
    // O motor usa a API do OkHttp 3 (o okhttp-brotli puxaria o 4.x, que quebra a compilação).
    "com.squareup.okhttp3:okhttp:3.12.13",
    "org.chromium.net:cronet-api:105.5195.68",
)

allprojects {
    configurations.configureEach {
        resolutionStrategy.force(engineForcedVersions)
        resolutionStrategy.eachDependency {
            // O youtubeapi pede kotlinx-coroutines na versão do Kotlin (1.8.10), que não existe.
            if (requested.group == "org.jetbrains.kotlinx" && requested.name.startsWith("kotlinx-coroutines")) {
                useVersion(libs.versions.coroutines.get())
            }
        }
    }
}
