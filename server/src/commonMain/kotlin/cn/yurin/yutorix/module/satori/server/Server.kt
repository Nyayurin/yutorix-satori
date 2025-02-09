@file:Suppress("MemberVisibilityCanBePrivate", "unused", "HttpUrlsUsage")

package cn.yurin.yutorix.module.satori.server

import cn.yurin.yutori.*
import kotlinx.atomicfu.atomic

fun Server.Companion.satori(
	alias: String? = null,
	listen: String = "0.0.0.0",
	port: Int = 8080,
	path: String = "",
	token: String? = null,
	version: String = "v1",
) = SatoriServer(alias, listen, port, path, token, version)

class SatoriServer(
	alias: String?,
	val listen: String,
	val port: Int,
	val path: String,
	val token: String?,
	val version: String,
) : Server(alias), Reinstallable {
	private var connecting by atomic(false)
	private var service: SatoriServerService? by atomic(null)

	override fun install(builder: YutoriBuilder) {}

	override fun uninstall(builder: YutoriBuilder) {}

	override suspend fun start(yutori: Yutori) {
		val properties = SatoriServerProperties(listen, port, path, token, version)
		connecting = true
		service = SatoriServerService(alias, properties, yutori)
		service!!.start()
	}

	override fun stop(yutori: Yutori) {
		connecting = false
		service?.stop()
		service = null
	}

	override suspend fun pushEvent(event: Event<SigningEvent>) = service!!.pushEvent(event)
}