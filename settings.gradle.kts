// settings.gradle.kts - CORRECTED

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

        // 🚨 USE THE FUNCTION CALL SYNTAX 🚨
        maven { setUrl("https://jitpack.io") }
    }
}

rootProject.name = "WasteSegregationApp"
include(":app")