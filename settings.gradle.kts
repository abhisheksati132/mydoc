// MyDoc 2.0.5 Multi-Module Project Configuration
// Modules: :app (Main Application), :lint-rules (Accessibility & Quality Lint), :office-engine (Decoupled Office Companion)
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = java.net.URI("https://maven.ghostscript.com/")
        }
    }
}

rootProject.name = "mydoc"
include(":app")
include(":lint-rules")
include(":office-engine")
include(":macrobenchmark")
