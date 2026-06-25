group = "app.revanced"

patches {
    about {
        name = "Custom ReVanced Patches"
        description = "Custom patches for ReVanced"
        source = "git@github.com:lluni/patches-template.git"
        author = "lluni"
        contact = "contact@revanced.app"
        website = "https://revanced.app"
        license = "GNU General Public License v3.0"
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}
