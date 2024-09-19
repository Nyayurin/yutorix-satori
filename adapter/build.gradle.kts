import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.serialization)
    alias(libs.plugins.atomicfu)
    alias(libs.plugins.android.library)
}

kotlin {
    jvmToolchain(17)

    jvm()

    androidTarget {
        publishLibraryVariants("release", "debug")
    }

    js {
        browser {
            webpackTask {
                mainOutputFileName = "yutorix-satori-adapter.js"
            }
        }
        nodejs()
        binaries.library()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            webpackTask {
                mainOutputFileName = "yutorix-satori-adapter.js"
            }
        }
        nodejs()
        binaries.library()
    }

    // Apple(IOS & MacOS)
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
        macosX64(),
        macosArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "yutorix-satori-adapter"
            isStatic = true
        }
    }

    // Linux
    linuxX64 {
        binaries.staticLib {
            baseName = "yutorix-satori-adapter"
        }
    }

    // Windows
    mingwX64 {
        binaries.staticLib {
            baseName = "yutorix-satori-adapter"
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.yutori)
            implementation(project(":yutorix-satori-core"))
            api(libs.ktor.client.core)
            api(libs.ktor.client.content.negotiation)
        }

        jvmMain.dependencies {
            api(libs.ktor.client.okhttp)
        }

        androidMain.dependencies {
            api(libs.ktor.client.okhttp)
        }

        jsMain.dependencies {
            api(libs.ktor.client.js)
        }

        wasmJsMain.dependencies {
            api(libs.ktor.client.js)
        }

        nativeMain.dependencies {
        }

        appleMain.dependencies {
            api(libs.ktor.client.darwin)
        }

        iosMain.dependencies {
        }

        macosMain.dependencies {
        }

        linuxMain.dependencies {
            api(libs.ktor.client.curl)
        }

        mingwMain.dependencies {
            api(libs.ktor.client.winhttp)
        }
    }
}

android {
    namespace = "cn.yurn.yutorix.satori.adapter"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

publishing {
    publications.withType<MavenPublication> {
        pom {
            name = "Yutorix-Satori-Adapter"
            version = System.getenv("VERSION")
            description = "Kotlin Multiplatform library"
            url = "https://github.com/Nyayurn/yutorix-satori"

            developers {
                developer {
                    id = "Nyayurn"
                    name = "Yurn"
                    email = "Nyayurn@outlook.com"
                }
            }
            scm {
                url = "https://github.com/Nyayurn/yutorix-satori"
            }
        }
    }
}