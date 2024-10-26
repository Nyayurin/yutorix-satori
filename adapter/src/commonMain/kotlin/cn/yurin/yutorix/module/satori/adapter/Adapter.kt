@file:Suppress("MemberVisibilityCanBePrivate", "unused", "HttpUrlsUsage")

package cn.yurin.yutorix.module.satori.adapter

import cn.yurin.yutori.Adapter
import cn.yurin.yutori.BuilderMarker
import cn.yurin.yutori.Login
import cn.yurin.yutori.Reinstallable
import cn.yurin.yutori.Yutori
import cn.yurin.yutorix.module.satori.SatoriAdapterProperties
import co.touchlab.kermit.Logger
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.delay

fun Adapter.Companion.satori(alias: String? = null) = SatoriAdapter(alias)

@BuilderMarker
class SatoriAdapter(
    alias: String?,
) : Adapter(alias),
    Reinstallable {
    var host: String = "127.0.0.1"
    var port: Int = 8080
    var path: String = ""
    var token: String? = null
    var version: String = "v1"
    var onStart: suspend WebSocketEventService.() -> Unit = { }
    var onConnect: suspend WebSocketEventService.(List<Login>) -> Unit = { }
    var onClose: suspend () -> Unit = { }
    var onError: suspend () -> Unit = { }
    private var connecting by atomic(false)
    private var service: WebSocketEventService? by atomic(null)

    fun onStart(block: suspend WebSocketEventService.() -> Unit) {
        onStart = block
    }

    fun onConnect(block: suspend WebSocketEventService.(List<Login>) -> Unit) {
        onConnect = block
    }

    fun onClose(block: suspend () -> Unit) {
        onClose = block
    }

    fun onError(block: suspend () -> Unit) {
        onError = block
    }

    override fun install(yutori: Yutori) {}

    override fun uninstall(yutori: Yutori) {}

    override suspend fun start(yutori: Yutori) {
        val properties = SatoriAdapterProperties(host, port, path, token, version)
        connecting = true
        var sequence: Number? = null
        do {
            service =
                WebSocketEventService(
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
                Logger.i(yutori.name) { "将在5秒后尝试重新连接" }
                delay(5000)
                Logger.i(yutori.name) { "尝试重新连接" }
            }
        } while (connecting)
    }

    override fun stop(yutori: Yutori) {
        connecting = false
        service?.disconnect()
        service = null
    }
}
