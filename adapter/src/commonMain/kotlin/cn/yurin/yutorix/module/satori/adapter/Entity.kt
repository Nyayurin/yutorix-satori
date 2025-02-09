package cn.yurin.yutorix.module.satori.adapter

data class SatoriAdapterProperties(
	val host: String = "127.0.0.1",
	val port: Int = 8080,
	val path: String = "",
	val token: String? = null,
	val version: String = "v1",
)