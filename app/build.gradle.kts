import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.hyper.music"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.hyper.music"
        minSdk = 33
        targetSdk = 37
        versionCode = 5
        versionName = "1.0.4"
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    androidResources {
        localeFilters += listOf("zh-rCN")
    }

    signingConfigs {
        create("release") {

            val props = Properties().apply {
                val f = rootProject.file("local.properties")
                if (f.exists()) f.inputStream().use { load(it) }
            }
            fun secret(key: String, fallback: String): String =
                props.getProperty(key) ?: System.getenv(key) ?: fallback

            storeFile = file(secret("HYPER_STORE_FILE", "../_local/hyper-release.jks"))
            storePassword = secret("HYPER_STORE_PASSWORD", "hyper123")
            keyAlias = secret("HYPER_KEY_ALIAS", "hyper")
            keyPassword = secret("HYPER_KEY_PASSWORD", "hyper123")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"

            excludes += "META-INF/**/LICENSE"
            excludes += "META-INF/**/LICENSE.txt"
            excludes += "META-INF/**/NOTICE"
            excludes += "META-INF/**/NOTICE.txt"

            excludes += "**/*.kotlin_builtins"

            excludes += "DebugProbesKt.bin"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)

            freeCompilerArgs.add("-nowarn")
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

composeCompiler {
    stabilityConfigurationFiles.set(listOf(project.layout.projectDirectory.file("compose-stability.conf")))
}

configurations.all {
    exclude(group = "top.yukonga.miuix.kmp", module = "miuix-ui-android")
    exclude(group = "top.yukonga.miuix.kmp", module = "miuix-core-android")
    exclude(group = "top.yukonga.miuix.kmp", module = "miuix-blur-android")
    exclude(group = "top.yukonga.miuix.kmp", module = "miuix-shader-android")
    exclude(group = "top.yukonga.miuix.kmp", module = "miuix-squircle-android")
    exclude(group = "top.yukonga.miuix.kmp", module = "miuix-preference-android")
}

dependencies {

    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.activity.compose)

    implementation(libs.androidx.compose.ui)

    implementation(libs.androidx.compose.ui.graphics)

    implementation("androidx.compose.foundation:foundation:1.12.0")
    implementation("androidx.compose.foundation:foundation-layout:1.12.0")
    implementation("androidx.compose.animation:animation:1.12.0")
    implementation("androidx.compose.animation:animation-core:1.12.0")
    implementation("androidx.compose.ui:ui:1.12.0")
    implementation("androidx.compose.ui:ui-graphics:1.12.0")
    implementation("androidx.compose.ui:ui-text:1.12.0")
    implementation("androidx.compose.ui:ui-unit:1.12.0")
    implementation("androidx.compose.ui:ui-geometry:1.12.0")
    implementation("androidx.compose.ui:ui-util:1.12.0")

    implementation(libs.androidx.core.ktx)

    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(libs.miuix.icons)
    implementation(libs.miuix.nav)

    implementation(libs.navigationevent.compose)

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.1")
    implementation("androidx.savedstate:savedstate-compose:1.3.0")

    implementation("org.jetbrains:annotations:26.1.0")

    implementation("androidx.media3:media3-exoplayer:1.10.1")

    implementation("androidx.media3:media3-session:1.10.1")

    implementation("androidx.media3:media3-datasource:1.10.1")

    implementation("androidx.media3:media3-database:1.10.1")

    implementation("androidx.documentfile:documentfile:1.0.1")

    debugImplementation(libs.androidx.compose.ui.tooling)
}
