@file:Suppress("MemberVisibilityCanBePrivate", "HttpUrlsUsage")

package cn.yurn.yutori.module.satori.adapter

import cn.yurn.yutori.Context
import cn.yurn.yutori.Event
import cn.yurn.yutori.EventService
import cn.yurn.yutori.Login
import cn.yurn.yutori.MessageEvents
import cn.yurn.yutori.RootActions
import cn.yurn.yutori.SatoriProperties
import cn.yurn.yutori.SigningEvent
import cn.yurn.yutori.Yutori
import cn.yurn.yutori.module.satori.EventSignal
import cn.yurn.yutori.module.satori.Identify
import cn.yurn.yutori.module.satori.IdentifySignal
import cn.yurn.yutori.module.satori.PingSignal
import cn.yurn.yutori.module.satori.PongSignal
import cn.yurn.yutori.module.satori.ReadySignal
import cn.yurn.yutori.module.satori.Signal
import cn.yurn.yutori.nick
import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.sendSerialized
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.websocket.close
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * Satori 事件服务的 WebSocket 实现
 * @param properties Satori Server 配置
 * @param yutori Satori 配置
 */
class WebSocketEventService(
    val properties: SatoriProperties,
    val onConnect: suspend WebSocketEventService.(List<Login>) -> Unit = { },
    val onClose: suspend () -> Unit = { },
    val onError: suspend () -> Unit = { },
    val yutori: Yutori,
    var sequence: Number? = null
) : EventService {
    var actionsList: List<RootActions>? = null
    val service = SatoriActionService(properties, yutori.name)
    private var isReceivedPong by atomic(false)
    private var job by atomic<Job?>(null)

    override suspend fun connect() {
        coroutineScope {
            job = launch {
                val client = HttpClient {
                    install(WebSockets) {
                        contentConverter = KotlinxWebsocketSerializationConverter(Json {
                            ignoreUnknownKeys = true
                        })
                    }
                }
                val name = yutori.name
                client.webSocket(
                    HttpMethod.Get,
                    properties.host,
                    properties.port,
                    "${properties.path}/${properties.version}/events"
                ) {
                    try {
                        var ready = false
                        sendSerialized(IdentifySignal(Identify(properties.token, sequence)))
                        Logger.i(name) { "成功建立 WebSocket 连接, 尝试建立事件推送服务" }
                        launch {
                            delay(10000)
                            if (!ready) throw RuntimeException("无法建立事件推送服务: READY 响应超时")
                            while (isActive) {
                                sendSerialized(PingSignal())
                                Logger.d(name) { "发送 PING" }
                                delay(10000)
                            }
                            println("Done")
                        }
                        launch {
                            do {
                                isReceivedPong = false
                                delay(1000 * 60)
                                if (!isReceivedPong) {
                                    throw RuntimeException("WebSocket 连接断开: PONG 响应超时")
                                }
                            } while (isActive)
                        }
                        while (isActive) try {
                            when (val signal = receiveDeserialized<Signal>()) {
                                is ReadySignal -> {
                                    ready = true
                                    actionsList = buildList {
                                        for (login in signal.body.logins) {
                                            add(
                                                RootActions(
                                                    platform = login.platform!!,
                                                    selfId = login.selfId!!,
                                                    service = service,
                                                    yutori = yutori
                                                )
                                            )
                                        }
                                    }
                                    onConnect(signal.body.logins)
                                    Logger.i(name) { "成功建立事件推送服务: ${signal.body.logins}" }
                                }

                                is EventSignal -> launch { onEvent(signal.body) }
                                is PongSignal -> {
                                    isReceivedPong = true
                                    Logger.d(name) { "收到 PONG" }
                                }

                                else -> throw UnsupportedOperationException("Unsupported signal: $signal")
                            }
                        } catch (e: Exception) {
                            Logger.w(name, e) { "事件解析错误" }
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
            val context = Context(actionsList!!.find {
                    actions -> actions.platform == event.platform && actions.selfId == event.selfId
            }!!, event, yutori)
            yutori.adapter.container(context)
        } catch (e: Exception) {
            Logger.w(name, e) { "处理事件时出错: $event" }
        }
    }

    override fun disconnect() {
        job?.cancel("Event service disconnect")
    }
}