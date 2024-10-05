@file:Suppress("MemberVisibilityCanBePrivate", "unused", "HttpUrlsUsage")

package cn.yurn.yutori.module.satori.server

import cn.yurn.yutori.BuilderMarker
import cn.yurn.yutori.Event
import cn.yurn.yutori.Reinstallable
import cn.yurn.yutori.Server
import cn.yurn.yutori.SigningEvent
import cn.yurn.yutori.Yutori
import cn.yurn.yutori.module.satori.SatoriServerProperties
import kotlinx.atomicfu.atomic

fun Server.Companion.satori(alias: String? = null) = SatoriServer(alias)

@BuilderMarker
class SatoriServer(alias: String?) : Server(alias), Reinstallable {
    var listen: String = "0.0.0.0"
    var port: Int = 8080
    var path: String = ""
    var token: String? = null
    var version: String = "v1"
    private var connecting by atomic(false)
    private var service: SatoriServerService? by atomic(null)

    override fun install(yutori: Yutori) {}
    override fun uninstall(yutori: Yutori) {}

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