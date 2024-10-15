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

    // Apple(IOS & MacOS)
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
        macosX64(),
        macosArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "yutorix-satori-server"
            isStatic = true
        }
    }

    // Linux
    linuxX64 {
        binaries.staticLib {
            baseName = "yutorix-satori-server"
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.yutori)
            implementation(project(":yutorix-satori-core"))
            api(libs.ktor.server.core)
            api(libs.ktor.server.content.negotiation)
            api(libs.ktor.server.cio)
            api(libs.ktor.server.websockets)
        }

        iosX64Main.dependencies {
            implementation(libs.yutori.ios.x64)
        }

        iosArm64Main.dependencies {
            implementation(libs.yutori.ios.arm64)
        }

        iosSimulatorArm64Main.dependencies {
            implementation(libs.yutori.ios.simulator.arm64)
        }

        macosX64Main.dependencies {
            implementation(libs.yutori.macos.x64)
        }

        macosArm64Main.dependencies {
            implementation(libs.yutori.macos.arm64)
        }
    }
}

android {
    namespace = "cn.yurin.yutorix.satori.server"
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
            name = "Yutorix-Satori-Server"
            version = System.getenv("VERSION")
            description = "Kotlin Multiplatform library"
            url = "https://github.com/Nyayurin/yutorix-satori"

            developers {
                developer {
                    id = "Nyayurin"
                    name = "Yurin"
                    email = "Nyayurn@outlook.com"
                }
            }
            scm {
                url = "https://github.com/Nyayurin/yutorix-satori"
            }
        }
    }
}