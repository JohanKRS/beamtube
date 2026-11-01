pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // Os módulos do motor não declaram repositórios; tudo vem daqui.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "ytslim"

include(":app")

// Motor: MediaServiceCore + SharedModules (submódulos, não editar).
// O core_settings.gradle do MediaServiceCore lê gradle.ext.mediaServiceCoreRoot e procura
// o SharedModules ao lado dele (../SharedModules), por isso os dois ficam na raiz do repo.
(gradle as ExtensionAware).extra["mediaServiceCoreRoot"] = file("MediaServiceCore")
apply(from = "MediaServiceCore/core_settings.gradle")
