group = "app.revanced"
version = "1.0.0"

patches {
    about {
        name = "Perplexity Patches"
        description = "Forces the native Google Speech-to-Text engine in Perplexity."
        source = "https://github.com/dalapenko/perplexity-repatch"
        author = "dalapenko"
        contact = "dalapenko"
        website = "https://github.com/dalapenko/perplexity-repatch"
        license = "GNU General Public License v3.0"
    }
}

repositories {
    google()
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/revanced/revanced-patches")
        credentials {
            username = providers.gradleProperty("gpr.user")
                .getOrElse(System.getenv("GITHUB_ACTOR"))
            password = providers.gradleProperty("gpr.key")
                .getOrElse(System.getenv("GITHUB_ACTOR"))
        }
    }
}

dependencies {
    implementation("com.google.guava:guava:33.5.0-jre")
}
