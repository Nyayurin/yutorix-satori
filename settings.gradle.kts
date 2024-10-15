rootProject.name = "Yutorix-Satori"

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/Nyayurin/yutori")
            credentials {
                username = providers.gradleProperty("gpr.actor").orNull ?: System.getenv("GITHUB_ACTOR")
                password = providers.gradleProperty("gpr.token").orNull ?: System.getenv("GITHUB_TOKEN")
            }
        }
        google()
        mavenCentral()
    }
}

include(":core")
include(":adapter")
include(":server")
findProject(":core")!!.name = "yutorix-satori-core"
findProject(":adapter")!!.name = "yutorix-satori-adapter"
findProject(":server")!!.name = "yutorix-satori-server"