package cn.yurin.yutorix.module.satori.server

data class SatoriServerProperties(
	val listen: String = "0.0.0.0",
	val port: Int = 8080,
	val path: String = "",
	val token: String? = null,
	val version: String = "v1",
)