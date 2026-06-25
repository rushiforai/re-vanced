group = "app.revanced"

patches {
    about {
        name = "Bakasura Patches"
        description = "Emby patches for ReVanced"
        source = "git@github.com:BakasuraRCE/bakasura-patches.git"
        author = "BakasuraRCE"
        contact = "https://github.com/BakasuraRCE"
        website = "https://github.com/BakasuraRCE/bakasura-patches"
        license = "GNU General Public License v3.0"
    }
}

dependencies {
    // Required due to smali, or build fails. Can be removed once smali is bumped.
    implementation(libs.guava)

    implementation(libs.apksig)

    // Android API stubs defined here.
    compileOnly(project(":patches:stub"))
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xexplicit-backing-fields",
            "-Xcontext-parameters"
        )
    }
}

publishing {
    repositories {
        maven {
            name = "githubPackages"
            url = uri("https://maven.pkg.github.com/revanced/revanced-patches")
            credentials(PasswordCredentials::class)
        }
    }
}

apply(from = "strings-processing.gradle.kts")
