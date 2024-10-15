plugins {
    alias(libs.plugins.multiplatform) apply false
    alias(libs.plugins.serialization) apply false
    alias(libs.plugins.atomicfu) apply false
    alias(libs.plugins.android.library) apply false
}

subprojects {
    apply(plugin = "maven-publish")

    group = "cn.yurin.yutorix"

    configure<PublishingExtension> {
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/Nyayurin/yutorix-satori")
                credentials {
                    username = System.getenv("GITHUB_ACTOR")
                    password = System.getenv("GITHUB_TOKEN")
                }
            }
        }
    }
}