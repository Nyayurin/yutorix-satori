<div align="center">

# Yutorix-Satori

Yutori 的 Satori 扩展模块

| Module                 | Platforms                                                                                                                                         |
|------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| yutorix-satori-core    | JVM(17), Android(17), JS(Browser, Node), WasmJS(Browser, Node), IOS(x64, Arm64, SimulatorArm64), MacOS(x64, Arm64), Linux(x64, Arm64), Mingw(x64) |
| yutorix-satori-adapter | JVM(17), Android(17), JS(Browser, Node), WasmJS(Browser, Node), IOS(x64, Arm64, SimulatorArm64), MacOS(x64, Arm64), Linux(x64, Arm64), Mingw(x64) |
| yutorix-satori-server  | JVM(17), Android(17), IOS(x64, Arm64, SimulatorArm64), MacOS(x64, Arm64), Linux(x64, Arm64)                                                       |

</div>

## 如何使用

### 从 Kotlin 项目开始

添加仓库

build.gradle.kts

```kotlin
maven {
    url = uri("https://maven.pkg.github.com/Nyayurin/yutorix-satori")
    credentials {
        username = project.findProperty("gpr.actor") as String? ?: System.getenv("GITHUB_ACTOR")
        password = project.findProperty("gpr.token") as String? ?: System.getenv("GITHUB_TOKEN")
    } 
}
```

settings.gradle.kts

```kotlin
maven {     
    url = uri("https://maven.pkg.github.com/Nyayurin/yutorix-satori")
    credentials {
        username = providers.gradleProperty("gpr.actor").orNull ?: System.getenv("GITHUB_ACTOR")
        password = providers.gradleProperty("gpr.token").orNull ?: System.getenv("GITHUB_TOKEN")
    }
}
```

引入依赖

```kotlin
implementation(libs.yutorix.satori.core)
implementation(libs.yutorix.satori.adapter) // 可选
implementation(libs.yutorix.satori.server) // 可选
```

```toml
[versions]
yutorix-satori = "version"

[libraries]
yutorix-satori-core = { module = "cn.yurin.yutorix:yutorix-satori-core", version.ref = "yutorix-satori" }
yutorix-satori-adapter = { module = "cn.yurin.yutorix:yutorix-satori-adapter", version.ref = "yutorix-satori" }
yutorix-satori-server = { module = "cn.yurin.yutorix:yutorix-satori-server", version.ref = "yutorix-satori" }
```

请从 [Github Packages](https://github.com/Nyayurin?tab=packages&repo_name=yutorix-satori) 获取版本<br>
对于稳定版, 它的版本号应该和 Release 中的 tag 一致<br>
对于开发板, 它的版本号应该和 Commit 的完整 SHA 值一致<br>
要获取一个 Commit 的完整 SHA 值, 请在 Commits 页面对应 Commit 处点击 Commit ID 右侧的复制按钮(Copy full SHA for *******)

安装模块

```kotlin
install(Adapter.satori(
    host = "127.0.0.1",
    port = 8080,
    token = "token"
))
install(Server.satori(
    listen = "0.0.0.0",
    port = 8080,
    token = "token"
))
```