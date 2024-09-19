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
        }

        jvmMain.dependencies {
        }

        androidMain.dependencies {
        }

        nativeMain.dependencies {
        }

        appleMain.dependencies {
            implementation("cn.yurn.yutori:yutori:28c5725e0109690fe0ede780641f8832ed31e453")
        }

        iosMain.dependencies {
        }

        iosX64Main.dependencies {
            implementation("cn.yurn.yutori:yutori-iosx64:28c5725e0109690fe0ede780641f8832ed31e453")
        }

        iosArm64Main.dependencies {
            implementation("cn.yurn.yutori:yutori-iosarm64:28c5725e0109690fe0ede780641f8832ed31e453")
        }

        iosSimulatorArm64Main.dependencies {
            implementation("cn.yurn.yutori:yutori-iossimulatorarm64:28c5725e0109690fe0ede780641f8832ed31e453")
        }

        macosMain.dependencies {
        }

        macosX64Main.dependencies {
            implementation("cn.yurn.yutori:yutori-macosx64:28c5725e0109690fe0ede780641f8832ed31e453")
        }

        macosArm64Main.dependencies {
            implementation("cn.yurn.yutori:yutori-macosarm64:28c5725e0109690fe0ede780641f8832ed31e453")
        }

        linuxMain.dependencies {
        }
    }
}

android {
    namespace = "cn.yurn.yutorix.satori.server"
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