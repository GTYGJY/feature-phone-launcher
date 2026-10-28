import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// 【修改】签名配置：所有口令只从项目根目录的 keystore.properties 读取（该文件已被 .gitignore 排除）。
// 仓库里不再保留任何明文口令——读不到有效配置时，release 会产出未签名 APK，而不是用内置密码签名。
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}
val hasReleaseSigning = keystorePropertiesFile.exists() &&
        !keystoreProperties.getProperty("storeFile").isNullOrBlank() &&
        !keystoreProperties.getProperty("storePassword").isNullOrBlank() &&
        !keystoreProperties.getProperty("keyAlias").isNullOrBlank() &&
        !keystoreProperties.getProperty("keyPassword").isNullOrBlank()

if (!hasReleaseSigning) {
    logger.warn(
        "[签名] 未找到有效的 keystore.properties，release 将生成未签名 APK。" +
            "请参考 keystore.properties.template 配置后再打包发布。"
    )
}

android {
    namespace = "com.featurephone.launcher"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.featurephone.launcher"
        minSdk = 24
        targetSdk = 34
        // 【修改】版本号升一档：此前 debug/release、新旧构建共用 versionCode 2 / 1.1，
        // 在手机「应用信息」里无法区分装的是哪次构建，导致反复出现"改了没生效"的误判。
        // 以后每次可见变更就递增，配合「关于」页显示 versionCode 即可确认。
        versionCode = 6
        versionName = "1.2.3"
    }

    signingConfigs {
        // 【修改】只有在 keystore.properties 提供了完整配置时才创建 release 签名
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            isJniDebuggable = false
            // 【修改】没有有效签名配置时保持不签名（产物 app-release-unsigned.apk），
            // 避免把口令以任何形式写回仓库。
            signingConfig = if (hasReleaseSigning) signingConfigs.getByName("release") else null
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    // 【修改】material 仅作为主题父样式（Theme.MaterialComponents）使用，保留；
    // constraintlayout、cardview 在代码与布局中均未使用，已移除依赖。
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}
