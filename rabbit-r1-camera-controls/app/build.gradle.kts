plugins {
    id("com.android.application")
}

android {
    namespace = "dev.r1native.cameracontrols"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.r1native.cameracontrols"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    compileOnly("io.github.libxposed:api:101.0.0")
}
