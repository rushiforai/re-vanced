plugins {
    kotlin("jvm") version "2.3.20"
}

group = "com.ultrasandbox"
version = "1.0.0"

dependencies {
    compileOnly("app.revanced:patcher:22.0.1")
    compileOnly("com.android.tools.smali:smali-dexlib2:3.0.9")
}

kotlin {
    jvmToolchain(17)
}

tasks.jar {
    archiveBaseName.set("ultrasandbox-patches")
    from("src/main/resources")
}
