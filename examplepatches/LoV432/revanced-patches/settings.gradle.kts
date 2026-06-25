rootProject.name = "revanced-patches"

pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        google()
        maven {
            name = "githubPackages"
            url = uri("https://maven.pkg.github.com/revanced/revanced-patches-gradle-plugin")
            credentials(PasswordCredentials::class)
        }
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
    }
}

plugins {
    id("app.revanced.patches") version "1.0.0-dev.10"
}
