rootProject.name = "ultrasandbox-patches"

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        maven {
            url = uri("https://maven.pkg.github.com/revanced/registry")
            credentials {
                username =
                    providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
                password =
                    providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
        maven {
            url = uri("https://maven.pkg.github.com/revanced/registry")
            credentials {
                username =
                    providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
                password =
                    providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

include(":patches")
include(":extensions")
