plugins {
    id("com.android.library") version "8.9.3"
}

android {
    namespace = "com.ultrasandbox"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
