@file:Suppress("MemberVisibilityCanBePrivate", "HttpUrlsUsage")

package cn.yurin.yutorix.module.satori.adapter

import cn.yurin.yutori.AdapterContext
import cn.yurin.yutori.AdapterEventService
import cn.yurin.yutori.Event
import cn.yurin.yutori.Login
import cn.yurin.yutori.MessageEvents
import cn.yurin.yutori.RootActions
import cn.yurin.yutori.SigningEvent
import cn.yurin.yutori.Yutori
import cn.yurin.yutori.nick
import cn.yurin.yutorix.module.satori.EventSignal
import cn.yurin.yutorix.module.satori.Identify
import cn.yurin.yutorix.module.satori.IdentifySignal
import cn.yurin.yutorix.module.satori.PingSignal
import cn.yurin.yutorix.module.satori.PongSignal
import cn.yurin.yutorix.module.satori.ReadySignal
import cn.yurin.yutorix.module.satori.SatoriAdapterProperties
import cn.yurin.yutorix.module.satori.Signal
import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.sendSerialized
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class WebSocketEventService(
    alias: String?,
    val properties: SatoriAdapterProperties,
    val onConnect: suspend WebSocketEventService.(List<Login>) -> Unit = { },
    val onClose: suspend () -> Unit = { },
    val onError: suspend () -> Unit = { },
    val yutori: Yutori,
    var sequence: Number? = null
) : AdapterEventService(alias) {
    private val actionsList = mutableListOf<RootActions>()
    val service = SatoriActionService(yutori, properties)
    private var job by atomic<Job?>(null)
    private val json = Json {
        ignoreUnknownKeys = true
    }

    override suspend fun connect() {
        coroutineScope {
            job = launch {
                val client = HttpClient {
                    install(WebSockets) {
                        contentConverter = KotlinxWebsocketSerializationConverter(json)
                    }
                }
                val name = yutori.name
                try {
                    client.webSocket(
                        HttpMethod.Get,
                        properties.host,
                        properties.port,
                        "${properties.path}/${properties.version}/events"
                    ) {
                        try {
                            yutori.actionsList.removeAll(actionsList)
                            actionsList.clear()
                            var ready = false
                            var isReceivedPong: Boolean
                            sendSerialized(
                                IdentifySignal(
                                    Identify(
                                        properties.token,
                                        sequence?.toInt()
                                    )
                                )
                            )
                            Logger.i(name) { "成功建立 WebSocket 连接, 尝试建立事件推送服务" }
                            launch {
                                delay(10000)
                                if (!ready) throw RuntimeException("无法建立事件推送服务: READY 等待超时")
                                while (isActive) {
                                    sendSerialized(PingSignal())
                                    Logger.d(name) { "发送 PING" }
                                    delay(10000)
                                }
                            }
                            while (isActive) try {
                                val receive =
                                    (incoming.receive() as? Frame.Text ?: continue).readText()
                                Logger.d(name) { "接收信令: $receive" }
                                when (val signal = json.decodeFromString<Signal>(receive)) {
                                    is ReadySignal -> {
                                        ready = true
                                        for (login in signal.body.logins) {
                                            val actions = RootActions(
                                                alias = alias,
                                                platform = login.platform!!,
                                                userId = login.user!!.id,
                                                service = service,
                                                yutori = yutori
                                            )
                                            actionsList += actions
                                            yutori.actionsList += actions
                                        }
                                        onConnect(signal.body.logins.map {
                                            it.toUniverse(
                                                null,
                                                yutori
                                            )
                                        })
                                        Logger.i(name) { "成功建立事件推送服务: ${signal.body.logins}" }
                                        launch {
                                            do {
                                                isReceivedPong = false
                                                delay(1000 * 60)
                                                if (!isReceivedPong) {
                                                    throw RuntimeException("WebSocket 连接断开: PONG 等待超时")
                                                }
                                            } while (isActive)
                                        }
                                    }

                                    is EventSignal -> launch {
                                        onEvent(
                                            signal.body.toUniverse(
                                                alias,
                                                yutori
                                            )
                                        )
                                    }

                                    is PongSignal -> {
                                        isReceivedPong = true
                                        Logger.d(name) { "收到 PONG" }
                                    }

                                    else -> throw UnsupportedOperationException("Unsupported signal: $signal")
                                }
                            } catch (e: Exception) {
                                Logger.w(name, e) { "信令解析错误" }
                            }
                        } catch (e: Exception) {
                            if (e is CancellationException && e.message == "Event service disconnect") {
                                Logger.i(name) { "WebSocket 连接断开: 主动断开连接" }
                            } else {
                                onError()
                                Logger.w(name, e) { "WebSocket 连接断开" }
                            }
                            close()
                        }
                        this@WebSocketEventService.onClose()
                    }
                } catch (e: Exception) {
                    Logger.w(e) { "WebSocket 连接失败" }
                }
                client.close()
            }
        }
    }

    private suspend fun onEvent(event: Event<SigningEvent>) {
        val name = yutori.name
        try {
            when (event.type) {
                MessageEvents.CREATED -> Logger.i(name) {
                    buildString {
                        append("${event.platform}(${event.selfId}) 接收事件(${event.type}): ")
                        append("${event.nullableChannel!!.name}(${event.nullableChannel!!.id})")
                        append("-")
                        append("${event.nick()}(${event.nullableUser!!.id})")
                        append(": ")
                        append(event.nullableMessage!!.content)
                    }
                }

                else -> Logger.i(name) { "${event.platform}(${event.selfId}) 接收事件: ${event.type}" }
            }
            Logger.d(name) { "事件详细信息: $event" }
            sequence = event.id
            val actions = actionsList.find { actions ->
                actions.platform == event.platform && actions.userId == event.selfId
            } ?: run {
                val actions = RootActions(
                    alias = alias,
                    platform = event.platform,
                    userId = event.selfId,
                    service = service,
                    yutori = yutori
                )
                actionsList += actions
                yutori.actionsList += actions
                actions
            }

            val context = AdapterContext(actions, event, yutori)
            yutori.adapter.container(context)
        } catch (e: Exception) {
            Logger.w(name, e) { "处理事件时出错: $event" }
        }
    }

    override fun disconnect() {
        job?.cancel("Event service disconnect")
    }
}