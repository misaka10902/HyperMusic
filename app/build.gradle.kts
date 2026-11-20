import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.hyper.music"
    compileSdk = 37
    // miuix 0.9.5-beta01 的 AAR 元数据要求 minCompileMinorSdk=2（即 API 37.2）
    compileSdkMinor = 2

    defaultConfig {
        applicationId = "com.hyper.music"
        minSdk = 33
        targetSdk = 37
        versionCode = 10
        versionName = "1.1.0"
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
        // 注意：不要用 packaging.dex.useLegacyPackaging 去压 dex 换体积 ——
        // dex 保持 Stored（AGP 默认）才能安装时直接 mmap，冷启动最快。
        // 体积只能靠 R8 真正裁代码，不做这种数字游戏。
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

// miuix 0.9.5-beta01 全部改用 maven artifact（glass 在 0.9.5 独立为 miuix-glass-android，
// GlassShape/GlassShadow 等移入 miuix-material-android）。自研的轻量组件以源码形式保留在
// com.hyper.music.ui 下及 top.yukonga.miuix.kmp.basic 中的 NativeTextField。

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

    // miuix-core 由 miuix-ui 以 compile scope 传递引入，这里不再重复声明。
    implementation(libs.miuix.ui)
    implementation(libs.miuix.material)
    implementation(libs.miuix.blur)
    implementation(libs.miuix.squircle)
    implementation(libs.miuix.preference)
    implementation(libs.miuix.glass)
    implementation(libs.miuix.icons)
    implementation(libs.miuix.glass.icons)
    implementation(libs.miuix.nav)

    implementation(libs.navigationevent.compose)

    // 说明：以下三项经源码 import 全量核对后确认未被直接使用，已移除：
    //   androidx.lifecycle:lifecycle-viewmodel-compose
    //   androidx.savedstate:savedstate-compose
    //   org.jetbrains:annotations（纯编译期注解，运行时无 class 依赖）
    // 若后续新增 ViewModel/SavedState 用法，再按需加回。

    implementation("androidx.media3:media3-exoplayer:1.10.1")

    implementation("androidx.media3:media3-session:1.10.1")

    implementation("androidx.media3:media3-datasource:1.10.1")

    implementation("androidx.media3:media3-database:1.10.1")

    implementation("androidx.documentfile:documentfile:1.0.1")

    debugImplementation(libs.androidx.compose.ui.tooling)
}
