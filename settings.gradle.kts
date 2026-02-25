pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    val agpVersion = buildscript.sourceFile!!.parentFile.resolve("gradle/libs.versions.toml")
        .readLines().first {
            it.startsWith("agp =")
        }.split("\"")[1]
    id("com.android.settings") version agpVersion
}

android {
    compileSdk {
        version = release(36)
    }
    targetSdk {
        version = release(36)
    }
    minSdk {
        version = release(26)
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "Androidify"

includeBuild("build-plugin")

include(":app")
include(":feature:launcher")
include(":core:util")
include(":core:theme")
