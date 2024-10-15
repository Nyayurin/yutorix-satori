@file:Suppress(
    "MemberVisibilityCanBePrivate",
    "unused",
    "HttpUrlsUsage",
    "UastIncorrectHttpHeaderInspection", "UNCHECKED_CAST"
)

package cn.yurin.yutorix.module.satori.adapter

import cn.yurn.yutori.AdapterActionService
import cn.yurn.yutori.Channel
import cn.yurn.yutori.FormData
import cn.yurn.yutori.Guild
import cn.yurn.yutori.GuildMember
import cn.yurn.yutori.GuildRole
import cn.yurn.yutori.Message
import cn.yurn.yutori.User
import cn.yurn.yutori.Yutori
import cn.yurn.yutori.message.element.MessageElement
import cn.yurin.yutorix.module.satori.SatoriAdapterProperties
import cn.yurin.yutorix.module.satori.SerializableBidiPagingList
import cn.yurin.yutorix.module.satori.SerializableChannel
import cn.yurin.yutorix.module.satori.SerializableGuild
import cn.yurin.yutorix.module.satori.SerializableGuildMember
import cn.yurin.yutorix.module.satori.SerializableGuildRole
import cn.yurin.yutorix.module.satori.SerializableLogin
import cn.yurin.yutorix.module.satori.SerializableMessage
import cn.yurin.yutorix.module.satori.SerializablePagingList
import cn.yurin.yutorix.module.satori.SerializableUser
import cn.yurin.yutorix.module.satori.serialize
import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import io.ktor.http.headers
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.core.use
import kotlinx.serialization.json.Json

class SatoriActionService(
    val yutori: Yutori,
    val properties: SatoriAdapterProperties
) : AdapterActionService() {
    override suspend fun send(
        resource: String,
        method: String,
        platform: String?,
        userId: String?,
        content: Map<String, Any?>
    ): Any = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
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
                        "Bearer ${properties.token}"
                    )
                }
                platform?.let { append("Satori-Platform", platform) }
                userId?.let { append("Satori-User-ID", userId) }
            }
            setBody(content.entries.filter { it.value != null }
                .joinToString(",", "{", "}") { (key, value) ->
                    buildString {
                        append("\"$key\":")
                        append(
                            when (value) {
                                is String -> "\"${value.replace("\"", "\\\"")}\""
                                is List<*> -> "\"${
                                    (value as List<MessageElement>).serialize()
                                        .replace("\n", "\\n")
                                        .replace("\"", "\\\"")
                                }\""

                                else -> value.toString()
                            }
                        )
                    }
                })
            Logger.d(yutori.name) {
                """
                Satori Action Request: url: ${this.url},
                    headers: ${this.headers.build()},
                    body: ${this.body}
                """.trimIndent()
            }
        }
        Logger.d(yutori.name) { "Satori Action Response: $response" }
        val body = response.bodyAsText()
        when (resource) {
            "channel" -> when (method) {
                "get" -> Json.decodeFromString<SerializableChannel>(body).toUniverse(null, yutori)
                "list" -> Json.decodeFromString<SerializablePagingList<SerializableChannel, Channel>>(body).toUniverse(null, yutori)
                "create" -> Json.decodeFromString<SerializableChannel>(body).toUniverse(null, yutori)
                "update" -> Unit
                "delete" -> Unit
                "mute" -> Unit
                else -> throw UnsupportedOperationException("Unsupported action: $resource.$method")
            }

            "user.channel" -> when (method) {
                "create" -> Json.decodeFromString<SerializableChannel>(body).toUniverse(null, yutori)
                else -> throw UnsupportedOperationException("Unsupported action: $resource.$method")
            }

            "guild" -> when (method) {
                "get" -> Json.decodeFromString<SerializableGuild>(body).toUniverse(null, yutori)
                "list" -> Json.decodeFromString<SerializablePagingList<SerializableGuild, Guild>>(body).toUniverse(null, yutori)
                "approve" -> Unit
                else -> throw UnsupportedOperationException("Unsupported action: $resource.$method")
            }

            "guild.member" -> when (method) {
                "get" -> Json.decodeFromString<SerializableGuildMember>(body).toUniverse(null, yutori)
                "list" -> Json.decodeFromString<SerializablePagingList<SerializableGuildMember, GuildMember>>(body).toUniverse(null, yutori)
                "kick" -> Unit
                "mute" -> Unit
                "approve" -> Unit
                else -> throw UnsupportedOperationException("Unsupported action: $resource.$method")
            }

            "guild.member.role" -> when (method) {
                "set" -> Unit
                "unset" -> Unit
                else -> throw UnsupportedOperationException("Unsupported action: $resource.$method")
            }

            "guild.role" -> when (method) {
                "list" -> Json.decodeFromString<SerializablePagingList<SerializableGuildRole, GuildRole>>(body).toUniverse(null, yutori)
                "create" -> Json.decodeFromString<SerializableGuildRole>(body).toUniverse(null, yutori)
                "update" -> Unit
                "delete" -> Unit
                else -> throw UnsupportedOperationException("Unsupported action: $resource.$method")
            }

            "login" -> when (method) {
                "get" -> Json.decodeFromString<SerializableLogin>(body).toUniverse(null, yutori)
                else -> throw UnsupportedOperationException("Unsupported action: $resource.$method")
            }

            "message" -> when (method) {
                "create" -> Json.decodeFromString<List<SerializableMessage>>(body).map { it.toUniverse(null, yutori) }
                "get" -> Json.decodeFromString<SerializableMessage>(body).toUniverse(null, yutori)
                "delete" -> Unit
                "update" -> Unit
                "list" -> Json.decodeFromString<SerializableBidiPagingList<SerializableMessage, Message>>(body).toUniverse(null, yutori)
                else -> throw UnsupportedOperationException("Unsupported action: $resource.$method")
            }

            "reaction" -> when (method) {
                "create" -> Unit
                "delete" -> Unit
                "clear" -> Unit
                "list" -> Json.decodeFromString<SerializablePagingList<SerializableUser, User>>(body).toUniverse(null, yutori)
                else -> throw UnsupportedOperationException("Unsupported action: $resource.$method")
            }

            "user" -> when (method) {
                "get" -> Json.decodeFromString<SerializableUser>(body).toUniverse(null, yutori)
                else -> throw UnsupportedOperationException("Unsupported action: $resource.$method")
            }

            "friend" -> when (method) {
                "list" -> Json.decodeFromString<SerializablePagingList<SerializableUser, User>>(body).toUniverse(null, yutori)
                "approve" -> Unit
                else -> throw UnsupportedOperationException("Unsupported action: $resource.$method")
            }

            else -> throw UnsupportedOperationException("Unsupported action: $resource.$method")
        }
    }

    override suspend fun upload(
        resource: String,
        method: String,
        platform: String,
        userId: String,
        content: List<FormData>
    ): Map<String, String> =
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }.use { client ->
            val url = URLBuilder().apply {
                host = properties.host
                port = properties.port
                appendPathSegments(properties.path, properties.version, "$resource.$method")
                headers {
                    properties.token?.let {
                        append(
                            HttpHeaders.Authorization,
                            "Bearer ${properties.token}"
                        )
                    }
                    append("Satori-Platform", platform)
                    append("Satori-User-ID", userId)
                }
            }.buildString()
            val formData = formData {
                for (data in content) {
                    append(data.name, data.content, Headers.build {
                        data.filename?.let { filename ->
                            append(HttpHeaders.ContentDisposition, "filename=\"$filename\"")
                        }
                        append(HttpHeaders.ContentType, data.type)
                    })
                }
            }
            Logger.d(yutori.name) {
                """
                Satori Action Request: url: $url,
                    body: $formData
                """.trimIndent()
            }
            val response = client.submitFormWithBinaryData(url, formData)
            Logger.d(yutori.name) { "Satori Action Response: $response" }
            response.body()
        }
}