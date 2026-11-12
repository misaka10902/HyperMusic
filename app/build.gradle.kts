// Hyper音乐 - 应用模块构建配置
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
        versionCode = 1
        versionName = "1.0.0"
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }
    // 资源过滤：语言只保留简体中文，去掉依赖库自带的其它语言翻译
    androidResources {
        localeFilters += listOf("zh-rCN")
    }

    signingConfigs {
        create("release") {
            // 签名信息不再写死在仓库里：优先读 local.properties / 环境变量，
            // 都没有时回退到仓库内的默认 keystore（仅本地构建用）。
            // 公开仓库请把 hyper-release.jks 与 local.properties 加入 .gitignore，
            // 并另外妥善备份 keystore —— 一旦丢失就无法对已发布的应用做增量更新。
            // 注意：这里不能写 java.util.Properties——Kotlin DSL 里的 `java` 会被
            // Android 扩展的 java 属性解析掉，必须在文件顶部 import。
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
            // 依赖库内嵌的许可证文本一并排除，减少安装包体积
            excludes += "META-INF/**/LICENSE"
            excludes += "META-INF/**/LICENSE.txt"
            excludes += "META-INF/**/NOTICE"
            excludes += "META-INF/**/NOTICE.txt"
            // Kotlin 元数据（.kotlin_builtins）：只有 kotlin-reflect 才需要，
            // 本项目没用反射，运行时用不到，纯占体积（约 30KB）
            excludes += "**/*.kotlin_builtins"
            // 协程调试探针：只在 IDE attach 调试时用得上，正式包排除
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
            // 内置的 miuix 快照源码用 @file:Suppress("INVISIBLE_REFERENCE") 引用 Compose 内部 API，
            // 新版 Kotlin 会对其发出「抑制未定义行为」的告警；该注解不能删（否则 DragGestureNode
            // 等内部类引用会直接报错），且其诊断被归为 error 级、无法用 -Xwarning-level 单独降级。
            // 这是第三方库自身的有意写法，故全局关闭告警输出（仅屏蔽 warning，不会屏蔽 error）。
            // 项目自有代码已用 @Suppress 精准抑制各自的弃用告警，与此处无关。
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

// miuix 快照版（0.9.4-ea1b935d-SNAPSHOT）已整套内置为本地源码：
// core / shader / blur / squircle / ui / preference / glass。
// 仅保留 icons 与 nav 两个 AAR（项目仍直接引用），并切断它们的传递依赖，
// 避免把旧正式版的 ui / blur / core 类带进来造成重复定义。
configurations.all {
    exclude(group = "top.yukonga.miuix.kmp", module = "miuix-ui-android")
    exclude(group = "top.yukonga.miuix.kmp", module = "miuix-core-android")
    exclude(group = "top.yukonga.miuix.kmp", module = "miuix-blur-android")
    exclude(group = "top.yukonga.miuix.kmp", module = "miuix-shader-android")
    exclude(group = "top.yukonga.miuix.kmp", module = "miuix-squircle-android")
    exclude(group = "top.yukonga.miuix.kmp", module = "miuix-preference-android")
}

dependencies {
    // ===== AndroidX / Compose 基础 =====
    // Compose BOM：统一管理所有 Compose 库版本
    implementation(platform(libs.androidx.compose.bom))
    // Activity 与 Compose 集成（setContent 入口）
    implementation(libs.androidx.activity.compose)
    // Compose UI 核心运行时
    implementation(libs.androidx.compose.ui)
    // Compose 图形模块（Canvas、绘制等）
    implementation(libs.androidx.compose.ui.graphics)
    // 快照版 miuix 源码内置后，foundation / animation 不再由 AAR 传递引入，显式声明。
    // 玻璃组件依赖 Compose 1.12.0 新增的 LayerOutsets 等 API，故锁定 1.12.0。
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
    // AndroidX 核心 KTX 扩展
    implementation(libs.androidx.core.ktx)
    // 生命周期感知型运行时 KTX
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // ===== Miuix（0.9.4-ea1b935d-SNAPSHOT 快照版，整套源码内置）=====
    // core / shader / blur / squircle / ui / preference / glass 均为 app/src/main/java 下的源码。
    // 仅图标与导航栈仍沿用 AAR（传递依赖已排除，避免与内置源码重复）。
    implementation(libs.miuix.icons)
    implementation(libs.miuix.nav)

    // ===== NavigationEvent（快照 ui / glass 需要 1.2.0）=====
    implementation(libs.navigationevent.compose)
    // nav AAR 的运行时依赖（其传递依赖被排除后显式补齐）
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.1")
    implementation("androidx.savedstate:savedstate-compose:1.3.0")

    // ===== 形状库 =====
    // 圆角/胶囊形状统一使用 miuix-glass 提供的 GlassShape / GlassCapsule，已移除 com.kyant.capsule
    // AGSL 运行时需要 org.jetbrains 注解（org.intellij.lang.annotations.Language）
    implementation("org.jetbrains:annotations:26.1.0")

    // ===== 在线音乐播放 =====
    // Media3 ExoPlayer：在线流媒体播放
    implementation("androidx.media3:media3-exoplayer:1.10.1")
    // Media3 Session：MediaSession / 系统播放控件（通知栏、锁屏、蓝牙）
    implementation("androidx.media3:media3-session:1.10.1")
    // Media3 Datasource：CacheDataSource / SimpleCache，用于边播放边缓存
    implementation("androidx.media3:media3-datasource:1.10.1")
    // Media3 Database：StandaloneDatabaseProvider，SimpleCache 的索引库
    implementation("androidx.media3:media3-database:1.10.1")

    // ===== 文件选择器 =====
    // 下载位置选择：用系统（小米澎湃OS 定制的）文件选择器选中目录后写入
    implementation("androidx.documentfile:documentfile:1.0.1")

    // ===== 调试专用 =====
    // Compose UI Tooling：Layout Inspector
    debugImplementation(libs.androidx.compose.ui.tooling)
}


