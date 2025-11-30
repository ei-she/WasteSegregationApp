// settings.gradle.kts - CORRECTED

pluginManagement {
    repositories {
        // 1. Ensure Google repository is explicitly listed for plugins
        google()

        // This 'content' block is often used to restrict searches,
        // but 'google()' above ensures the right place is searched.
        /* google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        */

        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 2. Ensure Google repository is listed for dependencies
        google()
        mavenCentral()

        // Use the function call syntax for custom repos
        maven { setUrl("https://jitpack.io") }
    }
}

rootProject.name = "WasteSegregationApp"
include(":app")