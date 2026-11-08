plugins { alias(libs.plugins.android.application) }

@org.gradle.work.DisableCachingByDefault(because = "Pinned Office binaries use a verified download cache")
abstract class PrepareOfficeEngineTask : Exec() {
    @get:org.gradle.api.tasks.OutputDirectory abstract val nativeDirectory: org.gradle.api.file.DirectoryProperty
    @get:org.gradle.api.tasks.OutputDirectory abstract val assetDirectory: org.gradle.api.file.DirectoryProperty
}

android {
    namespace = "com.abhisheksati.mydoc.officeengine"
    compileSdk = 37
    ndkVersion = "29.0.14206865"
    defaultConfig {
        applicationId = "com.abhisheksati.mydoc.officeengine"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "26.2.6.3-1"
    }
    signingConfigs {
        create("shared") {
            storeFile = rootProject.file("app/keystore/debug.keystore")
            storePassword = "android"; keyAlias = "mydoc"; keyPassword = "android"
        }
        create("release") {
            val path = System.getenv("MYDOC_KEYSTORE_PATH") ?: project.findProperty("mydoc.keystore.path") as? String
            if (path != null && file(path).exists()) {
                storeFile = file(path)
                storePassword = System.getenv("MYDOC_KEYSTORE_PASSWORD") ?: project.findProperty("mydoc.keystore.password") as? String
                keyAlias = System.getenv("MYDOC_KEY_ALIAS") ?: project.findProperty("mydoc.key.alias") as? String
                keyPassword = System.getenv("MYDOC_KEY_PASSWORD") ?: project.findProperty("mydoc.key.password") as? String
            }
        }
    }
    buildTypes {
        debug { applicationIdSuffix = ".debug"; signingConfig = signingConfigs.getByName("shared") }
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    buildFeatures { buildConfig = true }
    packaging { jniLibs.useLegacyPackaging = true }
    splits { abi { isEnable = true; reset(); include("arm64-v8a", "armeabi-v7a", "x86_64"); isUniversalApk = true } }
}

val prepareEngine = tasks.register<PrepareOfficeEngineTask>("prepareOfficeEngine") {
    inputs.files(rootProject.file("scripts/prepare_word_engine.py"), rootProject.file("third_party/libreoffice/engine.lock.json"), rootProject.file("app/src/main/cpp/word_guard.c"))
    nativeDirectory.set(layout.buildDirectory.dir("generated/word-engine/lib"))
    assetDirectory.set(layout.buildDirectory.dir("generated/word-engine/assets"))
    commandLine(if (System.getProperty("os.name").startsWith("Windows")) "python" else "python3",
        rootProject.file("scripts/prepare_word_engine.py").absolutePath,
        "--sdk", androidComponents.sdkComponents.sdkDirectory.get().asFile.absolutePath, "--install-ndk",
        "--output", layout.buildDirectory.dir("generated/word-engine").get().asFile.absolutePath)
}
androidComponents.onVariants { variant ->
    variant.sources.assets?.addGeneratedSourceDirectory(prepareEngine) { it.assetDirectory }
    variant.sources.jniLibs?.addGeneratedSourceDirectory(prepareEngine) { it.nativeDirectory }
}
tasks.named("preBuild") { dependsOn(prepareEngine) }
dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.org.json)
}
