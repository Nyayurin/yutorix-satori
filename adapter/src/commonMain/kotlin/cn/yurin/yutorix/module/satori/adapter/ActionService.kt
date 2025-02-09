@file:Suppress(
	"MemberVisibilityCanBePrivate",
	"unused",
	"HttpUrlsUsage",
	"UastIncorrectHttpHeaderInspection",
)

package cn.yurin.yutorix.module.satori.adapter

import cn.yurin.yutori.*
import cn.yurin.yutori.message.element.MessageElement
import cn.yurin.yutorix.module.satori.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.core.*
import kotlinx.serialization.json.Json

class SatoriActionService(
	val yutori: Yutori,
	val properties: SatoriAdapterProperties,
) : AdapterActionService() {
	private suspend fun send(
		resource: String,
		method: String,
		platform: String?,
		userId: String?,
		contents: Map<String, Any?>,
	): String = HttpClient {
		install(ContentNegotiation) {
			json(
				Json {
					ignoreUnknownKeys = true
				},
			)
		}
	}.use { client ->
		val response = client.post {
			url {
				host = properties.host
				port = properties.port
				appendPathSegments(properties.path, properties.version, "$resource.$method")
			}
			contentType(ContentType.Application.Json)
			headers {
				properties.token?.let {
					append(
						HttpHeaders.Authorization,
						"Bearer ${properties.token}",
					)
				}
				platform?.let { append("Satori-Platform", platform) }
				userId?.let { append("Satori-User-ID", userId) }
			}
			setBody(
				contents.entries.filter { it.value != null }.joinToString(",", "{", "}") { (key, value) ->
					buildString {
						append("\"$key\":")
						append(
							when (value) {
								is String -> "\"${value.replace("\"", "\\\"")}\""
								else -> value.toString()
							},
						)
					}
				},
			)
			Log.d {
				"""
                        Action Request: url: ${this.url},
                            headers: ${this.headers.build()},
                            body: ${this.body}
                        """.trimIndent()
			}
		}
		Log.d { "Action Response: $response" }
		if (response.status.isSuccess()) {
			response.bodyAsText()
		} else {
			throw RuntimeException("Satori Action Error: ${response.status}, ${response.bodyAsText()}")
		}
	}

	private suspend inline fun <reified T : Convertable<U>, U> sendSerialize(
		resource: String, method: String, platform: String?, userId: String?, content: Map<String, Any?>
	): U = Json.decodeFromString<T>(send(resource, method, platform, userId, content)).toUniverse(null, yutori)

	private suspend inline fun <reified T : Convertable<U>, U> sendList(
		resource: String, method: String, platform: String?, userId: String?, content: Map<String, Any?>
	): List<U> = Json.decodeFromString<List<T>>(send(resource, method, platform, userId, content)).map {
		it.toUniverse(null, yutori)
	}

	override suspend fun channelGet(
		headerPlatform: String, headerUserId: String, channelId: String, contents: Array<out Pair<String, Any>>
	) = sendSerialize<SerializableChannel, Channel>(
		resource = "channel", method = "create", platform = headerPlatform, userId = headerUserId, content = mapOf(
			"channel_id" to channelId, *contents
		)
	)

	override suspend fun channelList(
		headerPlatform: String,
		headerUserId: String,
		guildId: String,
		next: String?,
		contents: Array<out Pair<String, Any>>
	) = sendSerialize<SerializablePagingList<SerializableChannel, Channel>, PagingList<Channel>>(
		resource = "channel", method = "list", platform = headerPlatform, userId = headerUserId, content = mapOf(
			"guild_id" to guildId, "next" to next, *contents
		)
	)

	override suspend fun channelCreate(
		headerPlatform: String,
		headerUserId: String,
		guildId: String,
		data: Channel,
		contents: Array<out Pair<String, Any>>
	) = sendSerialize<SerializableChannel, Channel>(
		resource = "channel", method = "create", platform = headerPlatform, userId = headerUserId, content = mapOf(
			"guild_id" to guildId, "data" to data.toSerializable(), *contents
		)
	)

	override suspend fun channelUpdate(
		headerPlatform: String,
		headerUserId: String,
		channelId: String,
		data: Channel,
		contents: Array<out Pair<String, Any>>
	) {
		send(
			resource = "channel", method = "update", platform = headerPlatform, userId = headerUserId, contents = mapOf(
				"channel_id" to channelId, "data" to data.toSerializable(), *contents
			)
		)
	}

	override suspend fun channelDelete(
		headerPlatform: String, headerUserId: String, channelId: String, contents: Array<out Pair<String, Any>>
	) {
		send(
			resource = "channel", method = "delete", platform = headerPlatform, userId = headerUserId, contents = mapOf(
				"channel_id" to channelId, *contents
			)
		)
	}

	override suspend fun channelMute(
		headerPlatform: String,
		headerUserId: String,
		channelId: String,
		duration: Number,
		contents: Array<out Pair<String, Any>>
	) {
		send(
			resource = "channel", method = "mute", platform = headerPlatform, userId = headerUserId, contents = mapOf(
				"channel_id" to channelId, "duration" to duration, *contents
			)
		)
	}

	override suspend fun userChannelCreate(
		headerPlatform: String,
		headerUserId: String,
		userId: String,
		guildId: String?,
		contents: Array<out Pair<String, Any>>
	) = sendSerialize<SerializableChannel, Channel>(
		resource = "user.channel", method = "create", platform = headerPlatform, userId = headerUserId, content = mapOf(
			"user_id" to userId, "guild_id" to guildId, *contents
		)
	)

	override suspend fun guildGet(
		headerPlatform: String, headerUserId: String, guildId: String, contents: Array<out Pair<String, Any>>
	) = sendSerialize<SerializableGuild, Guild>(
		resource = "guild", method = "get", platform = headerPlatform, userId = headerUserId, content = mapOf(
			"guild_id" to guildId, *contents
		)
	)

	override suspend fun guildList(
		headerPlatform: String, headerUserId: String, next: String?, contents: Array<out Pair<String, Any>>
	) = sendSerialize<SerializablePagingList<SerializableGuild, Guild>, PagingList<Guild>>(
		resource = "guild", method = "list", platform = headerPlatform, userId = headerUserId, content = mapOf(
			"next" to next, *contents
		)
	)

	override suspend fun guildApprove(
		headerPlatform: String,
		headerUserId: String,
		messageId: String,
		approve: Boolean,
		comment: String,
		contents: Array<out Pair<String, Any>>
	) {
		send(
			resource = "guild", method = "approve", platform = headerPlatform, userId = headerUserId, contents = mapOf(
				"message_id" to messageId, "approve" to approve, "comment" to comment, *contents
			)
		)
	}

	override suspend fun guildMemberGet(
		headerPlatform: String,
		headerUserId: String,
		guildId: String,
		userId: String,
		contents: Array<out Pair<String, Any>>
	) = sendSerialize<SerializableGuildMember, GuildMember>(
		resource = "guild.member", method = "get", platform = headerPlatform, userId = headerUserId, content = mapOf(
			"guild_id" to guildId, "user_id" to userId, *contents
		)
	)

	override suspend fun guildMemberList(
		headerPlatform: String,
		headerUserId: String,
		guildId: String,
		next: String?,
		contents: Array<out Pair<String, Any>>
	) = sendSerialize<SerializablePagingList<SerializableGuildMember, GuildMember>, PagingList<GuildMember>>(
		resource = "guild.member", method = "list", platform = headerPlatform, userId = headerUserId, content = mapOf(
			"guild_id" to guildId, "next" to next, *contents
		)
	)

	override suspend fun guildMemberKick(
		headerPlatform: String,
		headerUserId: String,
		guildId: String,
		userId: String,
		permanent: Boolean?,
		contents: Array<out Pair<String, Any>>
	) {
		send(
			resource = "guild.member",
			method = "kick",
			platform = headerPlatform,
			userId = headerUserId,
			contents = mapOf(
				"guild_id" to guildId, "user_id" to userId, "permanent" to permanent, *contents
			)
		)
	}

	override suspend fun guildMemberMute(
		headerPlatform: String,
		headerUserId: String,
		guildId: String,
		userId: String,
		duration: Number,
		contents: Array<out Pair<String, Any>>
	) {
		send(
			resource = "guild.member",
			method = "mute",
			platform = headerPlatform,
			userId = headerUserId,
			contents = mapOf(
				"guild_id" to guildId, "user_id" to userId, "duration" to duration, *contents
			)
		)
	}

	override suspend fun guildMemberApprove(
		headerPlatform: String,
		headerUserId: String,
		messageId: String,
		approve: Boolean,
		comment: String,
		contents: Array<out Pair<String, Any>>
	) {
		send(
			resource = "guild.member",
			method = "approve",
			platform = headerPlatform,
			userId = headerUserId,
			contents = mapOf(
				"message_id" to messageId, "approve" to approve, "comment" to comment, *contents
			)
		)
	}

	override suspend fun guildMemberRoleSet(
		headerPlatform: String,
		headerUserId: String,
		guildId: String,
		userId: String,
		roleId: String,
		contents: Array<out Pair<String, Any>>
	) {
		send(
			resource = "guild.member.role",
			method = "set",
			platform = headerPlatform,
			userId = headerUserId,
			contents = mapOf(
				"guild_id" to guildId, "user_id" to userId, "role_id" to roleId, *contents
			)
		)
	}

	override suspend fun guildMemberRoleUnset(
		headerPlatform: String,
		headerUserId: String,
		guildId: String,
		userId: String,
		roleId: String,
		contents: Array<out Pair<String, Any>>
	) {
		send(
			resource = "guild.member.role",
			method = "unset",
			platform = headerPlatform,
			userId = headerUserId,
			contents = mapOf(
				"guild_id" to guildId, "user_id" to userId, "role_id" to roleId, *contents
			)
		)
	}

	override suspend fun guildRoleList(
		headerPlatform: String,
		headerUserId: String,
		guildId: String,
		next: String?,
		contents: Array<out Pair<String, Any>>
	) = sendSerialize<SerializablePagingList<SerializableGuildRole, GuildRole>, PagingList<GuildRole>>(
		resource = "guild.role", method = "list", platform = headerPlatform, userId = headerUserId, content = mapOf(
			"guild_id" to guildId, "next" to next, *contents
		)
	)

	override suspend fun guildRoleCreate(
		headerPlatform: String,
		headerUserId: String,
		guildId: String,
		role: GuildRole,
		contents: Array<out Pair<String, Any>>
	) = sendSerialize<SerializableGuildRole, GuildRole>(
		resource = "guild.role", method = "create", platform = headerPlatform, userId = headerUserId, content = mapOf(
			"guild_id" to guildId, "role" to role.toSerializable(), *contents
		)
	)

	override suspend fun guildRoleUpdate(
		headerPlatform: String,
		headerUserId: String,
		guildId: String,
		roleId: String,
		role: GuildRole,
		contents: Array<out Pair<String, Any>>
	) {
		send(
			resource = "guild.role",
			method = "update",
			platform = headerPlatform,
			userId = headerUserId,
			contents = mapOf(
				"guild_id" to guildId, "role_id" to roleId, "role" to role.toSerializable(), *contents
			)
		)
	}

	override suspend fun guildRoleDelete(
		headerPlatform: String,
		headerUserId: String,
		guildId: String,
		roleId: String,
		contents: Array<out Pair<String, Any>>
	) {
		send(
			resource = "guild.role",
			method = "delete",
			platform = headerPlatform,
			userId = headerUserId,
			contents = mapOf(
				"guild_id" to guildId, "role_id" to roleId, *contents
			)
		)
	}

	override suspend fun loginGet(
		headerPlatform: String, headerUserId: String, contents: Array<out Pair<String, Any>>
	) = sendSerialize<SerializableLogin, Login>(
		resource = "login", method = "get", platform = headerPlatform, userId = headerUserId, content = mapOf(*contents)
	)

	override suspend fun messageCreate(
		headerPlatform: String,
		headerUserId: String,
		channelId: String,
		content: List<MessageElement>,
		contents: Array<out Pair<String, Any>>
	) = sendList<SerializableMessage, Message>(
		resource = "message", method = "create", platform = headerPlatform, userId = headerUserId, content = mapOf(
			"channel_id" to channelId, "content" to content.serialize(), *contents
		)
	)

	override suspend fun messageGet(
		headerPlatform: String,
		headerUserId: String,
		channelId: String,
		messageId: String,
		contents: Array<out Pair<String, Any>>
	) = sendSerialize<SerializableMessage, Message>(
		resource = "message", method = "get", platform = headerPlatform, userId = headerUserId, content = mapOf(
			"channel_id" to channelId, "message_id" to messageId, *contents
		)
	)

	override suspend fun messageDelete(
		headerPlatform: String,
		headerUserId: String,
		channelId: String,
		messageId: String,
		contents: Array<out Pair<String, Any>>
	) {
		send(
			resource = "message", method = "delete", platform = headerPlatform, userId = headerUserId, contents = mapOf(
				"channel_id" to channelId, "message_id" to messageId, *contents
			)
		)
	}

	override suspend fun messageUpdate(
		headerPlatform: String,
		headerUserId: String,
		channelId: String,
		messageId: String,
		content: List<MessageElement>,
		contents: Array<out Pair<String, Any>>
	) {
		send(
			resource = "message", method = "update", platform = headerPlatform, userId = headerUserId, contents = mapOf(
				"channel_id" to channelId, "message_id" to messageId, "content" to content.serialize(), *contents
			)
		)
	}

	override suspend fun messageList(
		headerPlatform: String,
		headerUserId: String,
		channelId: String,
		next: String?,
		direction: BidiPagingList.Direction?,
		limit: Number?,
		order: BidiPagingList.Order?,
		contents: Array<out Pair<String, Any>>
	) = sendSerialize<SerializableBidiPagingList<SerializableMessage, Message>, BidiPagingList<Message>>(
		resource = "message", method = "list", platform = headerPlatform, userId = headerUserId, content = mapOf(
			"channel_id" to channelId,
			"next" to next,
			"direction" to direction?.value,
			"limit" to limit,
			"order" to order?.value,
			*contents
		)
	)

	override suspend fun reactionCreate(
		headerPlatform: String,
		headerUserId: String,
		channelId: String,
		messageId: String,
		emoji: String,
		contents: Array<out Pair<String, Any>>
	) {
		send(
			resource = "reaction",
			method = "create",
			platform = headerPlatform,
			userId = headerUserId,
			contents = mapOf(
				"channel_id" to channelId, "message_id" to messageId, "emoji" to emoji, *contents
			)
		)
	}

	override suspend fun reactionDelete(
		headerPlatform: String,
		headerUserId: String,
		channelId: String,
		messageId: String,
		emoji: String,
		userId: String?,
		contents: Array<out Pair<String, Any>>
	) {
		send(
			resource = "reaction",
			method = "delete",
			platform = headerPlatform,
			userId = headerUserId,
			contents = mapOf(
				"channel_id" to channelId, "message_id" to messageId, "emoji" to emoji, "user_id" to userId, *contents
			)
		)
	}

	override suspend fun reactionClear(
		headerPlatform: String,
		headerUserId: String,
		channelId: String,
		messageId: String,
		emoji: String?,
		contents: Array<out Pair<String, Any>>
	) {
		send(
			resource = "reaction", method = "clear", platform = headerPlatform, userId = headerUserId, contents = mapOf(
				"channel_id" to channelId, "message_id" to messageId, "emoji" to emoji, *contents
			)
		)
	}

	override suspend fun reactionList(
		headerPlatform: String,
		headerUserId: String,
		channelId: String,
		messageId: String,
		emoji: String,
		next: String?,
		contents: Array<out Pair<String, Any>>
	) = sendSerialize<SerializablePagingList<SerializableUser, User>, PagingList<User>>(
		resource = "reaction", method = "list", platform = headerPlatform, userId = headerUserId, content = mapOf(
			"channel_id" to channelId, "message_id" to messageId, "emoji" to emoji, "next" to next, *contents
		)
	)

	override suspend fun userGet(
		headerPlatform: String, headerUserId: String, userId: String, contents: Array<out Pair<String, Any>>
	) = sendSerialize<SerializableUser, User>(
		resource = "user", method = "get", platform = headerPlatform, userId = headerUserId, content = mapOf(
			"user_id" to userId, *contents
		)
	)

	override suspend fun friendList(
		headerPlatform: String, headerUserId: String, next: String?, contents: Array<out Pair<String, Any>>
	) = sendSerialize<SerializablePagingList<SerializableUser, User>, PagingList<User>>(
		resource = "friend", method = "list", platform = headerPlatform, userId = headerUserId, content = mapOf(
			"next" to next, *contents
		)
	)

	override suspend fun friendApprove(
		headerPlatform: String,
		headerUserId: String,
		messageId: String,
		approve: Boolean,
		comment: String?,
		contents: Array<out Pair<String, Any>>
	) {
		send(
			resource = "friend", method = "approve", platform = headerPlatform, userId = headerUserId, contents = mapOf(
				"message_id" to messageId, "approve" to approve, "comment" to comment, *contents
			)
		)
	}

	override suspend fun adminLoginList(
		contents: Array<out Pair<String, Any>>
	) = sendList<SerializableLogin, Login>(
		resource = "admin.login", method = "list", platform = null, userId = null, content = mapOf(*contents)
	)

	override suspend fun adminWebhookCreate(
		url: String, token: String?, contents: Array<out Pair<String, Any>>
	) {
		send(
			resource = "admin.webhook", method = "create", platform = null, userId = null, contents = mapOf(
				"url" to url, "token" to token, *contents
			)
		)
	}

	override suspend fun adminWebhookDelete(
		url: String, contents: Array<out Pair<String, Any>>
	) {
		send(
			resource = "admin.webhook", method = "delete", platform = null, userId = null, contents = mapOf(
				"url" to url, *contents
			)
		)
	}

	override suspend fun uploadCreate(
		headerPlatform: String, headerUserId: String, contents: Array<out FormData>
	): Map<String, String> {
		return HttpClient {
			install(ContentNegotiation) {
				json(
					Json {
						ignoreUnknownKeys = true
					},
				)
			}
		}.use { client ->
			val url = URLBuilder().apply {
					host = properties.host
					port = properties.port
					appendPathSegments(properties.path, properties.version, "upload.create")
					headers {
						properties.token?.let {
							append(
								HttpHeaders.Authorization,
								"Bearer ${properties.token}",
							)
						}
						append("Satori-Platform", headerPlatform)
						append("Satori-User-ID", headerUserId)
					}
				}.buildString()
			val formData = formData {
				for (data in contents) {
					append(
						data.name,
						data.content,
						Headers.build {
							data.filename?.let { filename ->
								append(HttpHeaders.ContentDisposition, "filename=\"$filename\"")
							}
							append(HttpHeaders.ContentType, data.type)
						},
					)
				}
			}
			Log.d {
				"""
                Action Request: url: $url,
                    body: $formData
                """.trimIndent()
			}
			val response = client.submitFormWithBinaryData(url, formData)
			Log.d { "Action Response: $response" }
			if (response.status.isSuccess()) {
				response.body()
			} else {
				throw RuntimeException("Satori Action Error: ${response.status}, ${response.bodyAsText()}")
			}
		}
	}
}