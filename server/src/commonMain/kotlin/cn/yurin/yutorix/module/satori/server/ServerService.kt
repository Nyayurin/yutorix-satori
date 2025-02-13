@file:Suppress("MemberVisibilityCanBePrivate")

package cn.yurin.yutorix.module.satori.server

import cn.yurin.yutori.*
import cn.yurin.yutorix.module.satori.*
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.receiveDeserialized
import io.ktor.server.websocket.sendSerialized
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.close
import io.ktor.websocket.send
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class SatoriServerService(
	alias: String?,
	val properties: SatoriServerProperties,
	val yutori: Yutori,
) : ServerService(alias) {
	val logins = mutableListOf<Login>()
	val connectedClients = mutableListOf<WebSocketServerSession>()
	private var job by atomic<Job?>(null)
	private val json = Json {
		ignoreUnknownKeys = true
	}

	override suspend fun start() {
		coroutineScope {
			job = launch {
				embeddedServer(
					factory = CIO,
					host = properties.listen,
					port = properties.port,
				) {
					connectedClients.clear()
					install(ContentNegotiation) {
						json(json)
					}
					install(WebSockets) {
						contentConverter = KotlinxWebsocketSerializationConverter(json)
					}
					routing {
						webSocket("${properties.path}/${properties.version}/events") {
							var identify = false
							var isReceivedPing: Boolean
							val address = call.request.local.remoteAddress
							try {
								Log.i { "WebSocket 连接建立($address), 等待建立事件推送服务" }
								launch {
									delay(10000)
									if (!identify) {
										Log.i { "WebSocket 连接断开($address): IDENTIFY 等待超时" }
										close()
									}
								}
								while (isActive) {
									when (val signal = receiveDeserialized<Signal>()) {
										is IdentifySignal -> {
											identify = true
											if (signal.body.token != properties.token) {
												connectedClients -= this
												Log.i { "WebSocket 连接断开($address): token 错误" }
												close(CloseReason(3000, "Unauthorized"))
												return@webSocket
											}
											connectedClients += this
											Log.i { "建立事件推送服务($address): $logins" }
											sendSerialized(ReadySignal(Ready(logins.map { it.toSerializable() })))
											launch {
												do {
													isReceivedPing = false
													delay(1000 * 60)
													if (!isReceivedPing) {
														throw RuntimeException("WebSocket 连接断开($address): PING 等待超时")
													}
												} while (isActive)
											}
										}

										is PingSignal -> {
											isReceivedPing = true
											Log.d { "收到 PING" }
											sendSerialized(PongSignal())
										}

										else -> throw UnsupportedOperationException("Unsupported signal: $signal")
									}
								}
							} catch (e: Exception) {
								Log.i(e) { "WebSocket 连接断开($address)" }
							}
						}
						post("${properties.path}/${properties.version}/{api}") {
							val address = call.request.local.remoteAddress
							val api = "/" + call.pathParameters["api"]!!
							val platform = call.request.headers["Satori-Platform"]
							val selfId = call.request.headers["Satori-User-ID"]
							val content = call.receiveText()
							Log.i { "Action($address): $api($platform, $selfId), $content" }
							val token = call.request.headers["Authorization"]
							if (token == null) {
								call.respond(HttpStatusCode.Unauthorized)
								Log.i { "Action failed: Unauthorized" }
								return@post
							}
							if (!token.startsWith("Bearer ")) {
								call.respond(HttpStatusCode.BadRequest)
								Log.i { "Action failed: BadRequest" }
								return@post
							}
							val element = json.decodeFromString<JsonElement>(content).jsonObject.toMutableMap()
							val context = ServerContext(
								Request(
									alias = alias,
									api = api,
									header = mutableMapOf(
										"platform" to platform,
										"selfId" to selfId,
									),
									body = mutableMapOf(
										"channelId" to element.remove("channel_id")?.jsonPrimitive?.content,
										"guildId" to element.remove("guild_id")?.jsonPrimitive?.content,
										"next" to element.remove("next")?.jsonPrimitive?.content,
										"data" to element.remove("data")?.let {
											json.decodeFromJsonElement<SerializableChannel>(
												it,
											).toUniverse(null, yutori)
										},
										"duration" to element.remove("duration")?.jsonPrimitive?.int,
										"userId" to element.remove("user_id")?.jsonPrimitive?.content,
										"messageId" to element.remove("message_id")?.jsonPrimitive?.content,
										"approve" to element.remove("approve")?.jsonPrimitive?.boolean,
										"comment" to element.remove("comment")?.jsonPrimitive?.content,
										"permanent" to element.remove("permanent")?.jsonPrimitive?.boolean,
										"roleId" to element.remove("role_id")?.jsonPrimitive?.content,
										"role" to element.remove("role")?.let {
											json.decodeFromJsonElement<SerializableGuildRole>(
												it,
											)
										},
										"content" to element.remove("content")?.jsonPrimitive?.content?.deserialize(
											yutori,
										),
										"direction" to element.remove("direction")?.jsonPrimitive?.content?.let { direction ->
											BidiPagingList.Direction.entries.find { it.value == direction }
										},
										"limit" to element.remove("limit")?.jsonPrimitive?.int,
										"order" to element.remove("order")?.jsonPrimitive?.content.let { order ->
											BidiPagingList.Order.entries.find { it.value == order }
										},
										"emoji" to element.remove("emoji")?.jsonPrimitive?.content,
									).apply {
										putAll(element)
									}.toMap(),
								),
								Response { call.respond(it) },
								yutori,
							)
							yutori.serverConfig.container(context)
						}
					}
				}.start(wait = true)
			}
		}
	}

	override suspend fun pushEvent(event: Event<SigningEvent>) {
		coroutineScope {
			for (session in connectedClients) {
				launch {
					val json = json.encodeToString(EventSignal(event.toSerializable()))
					Log.d { "推送事件: $json" }
					session.send(json)
				}
			}
		}
	}

	override fun stop() {
		job?.cancel("Server service stop")
	}
}