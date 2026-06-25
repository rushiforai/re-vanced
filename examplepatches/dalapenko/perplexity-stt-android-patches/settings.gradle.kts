pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven {
            name = "githubPackages"
            url = uri("https://maven.pkg.github.com/revanced/revanced-patches-gradle-plugin")
            credentials {
                username = providers.gradleProperty("gpr.user")
                    .getOrElse(System.getenv("GITHUB_ACTOR"))
                password = providers.gradleProperty("gpr.key")
                    .getOrElse(System.getenv("GITHUB_ACTOR"))
            }
        }
    }
}

plugins {
    id("app.revanced.patches") version "1.0.0-dev.11"
}

rootProject.name = "perplexity-repatch"
