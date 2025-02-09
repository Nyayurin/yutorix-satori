@file:Suppress("MemberVisibilityCanBePrivate", "unused", "HttpUrlsUsage")

package cn.yurin.yutorix.module.satori.adapter

import cn.yurin.yutori.*
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.delay

fun Adapter.Companion.satori(
	alias: String? = null,
	host: String = "127.0.0.1",
	port: Int = 8080,
	path: String = "",
	token: String? = null,
	version: String = "v1",
	onStart: suspend WebSocketEventService.() -> Unit = { },
	onConnect: suspend WebSocketEventService.(List<Login>) -> Unit = { },
	onClose: suspend () -> Unit = { },
	onError: suspend () -> Unit = { },
) = SatoriAdapter(alias, host, port, path, token, version, onStart, onConnect, onClose, onError)

class SatoriAdapter(
	alias: String?,
	val host: String,
	val port: Int,
	val path: String,
	val token: String?,
	val version: String,
	val onStart: suspend WebSocketEventService.() -> Unit,
	val onConnect: suspend WebSocketEventService.(List<Login>) -> Unit,
	val onClose: suspend () -> Unit,
	val onError: suspend () -> Unit,
) : Adapter(alias), Reinstallable {
	private var connecting by atomic(false)
	private var service: WebSocketEventService? by atomic(null)

	override fun install(builder: YutoriBuilder) {}

	override fun uninstall(builder: YutoriBuilder) {}

	override suspend fun start(yutori: Yutori) {
		val properties = SatoriAdapterProperties(host, port, path, token, version)
		connecting = true
		var sequence: Number? = null
		do {
			service = WebSocketEventService(
				alias,
				properties,
				onConnect,
				onClose,
				onError,
				yutori,
				sequence,
			)
			service!!.onStart()
			service!!.connect()
			if (connecting) {
				sequence = (service as WebSocketEventService).sequence
				Log.i { "将在5秒后尝试重新连接" }
				delay(5000)
				Log.i { "尝试重新连接" }
			}
		} while (connecting)
	}

	override fun stop(yutori: Yutori) {
		connecting = false
		service?.disconnect()
		service = null
	}
}