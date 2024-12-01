@file:Suppress("UNCHECKED_CAST")

package cn.yurin.yutorix.module.satori

import cn.yurin.yutori.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable(SignalSerializer::class)
sealed class Signal {
    abstract val op: Int

    companion object {
        const val EVENT = 0
        const val PING = 1
        const val PONG = 2
        const val IDENTIFY = 3
        const val READY = 4
    }
}

@Serializable
data class EventSignal(
    val body: SerializableEvent,
) : Signal() {
    override val op: Int

    init {
        op = EVENT
    }
}

@Serializable
class PingSignal : Signal() {
    override val op: Int

    init {
        op = PING
    }
}

@Serializable
class PongSignal : Signal() {
    override val op: Int

    init {
        op = PONG
    }
}

@Serializable
data class IdentifySignal(
    val body: Identify,
) : Signal() {
    override val op: Int

    init {
        op = IDENTIFY
    }
}

@Serializable
data class ReadySignal(
    val body: Ready,
) : Signal() {
    override val op: Int

    init {
        op = READY
    }
}

@Serializable
data class Identify(
    val token: String? = null,
    val sequence: Int? = null,
)

@Serializable
data class Ready(
    val logins: List<SerializableLogin>,
)

data class SatoriAdapterProperties(
    val host: String = "127.0.0.1",
    val port: Int = 8080,
    val path: String = "",
    val token: String? = null,
    val version: String = "v1",
)

data class SatoriServerProperties(
    val listen: String = "0.0.0.0",
    val port: Int = 8080,
    val path: String = "",
    val token: String? = null,
    val version: String = "v1",
)

interface Convertable<U> {
    fun toUniverse(
        alias: String?,
        yutori: Yutori,
    ): U
}

@Serializable
class SerializableEvent(
    val id: Int,
    val type: String,
    val platform: String,
    @SerialName("self_id") val selfId: String,
    val timestamp: Long,
    val argv: SerializableInteraction.Argv? = null,
    val button: SerializableInteraction.Button? = null,
    val channel: SerializableChannel? = null,
    val guild: SerializableGuild? = null,
    val login: SerializableLogin? = null,
    val member: SerializableGuildMember? = null,
    val message: SerializableMessage? = null,
    val operator: SerializableUser? = null,
    val role: SerializableGuildRole? = null,
    val user: SerializableUser? = null,
) : Convertable<Event<SigningEvent>> {
    override fun toUniverse(
        alias: String?,
        yutori: Yutori,
    ) = Event<SigningEvent>(
        alias = alias,
        id = id,
        type = type,
        platform = platform,
        selfId = selfId,
        timestamp = timestamp,
        argv = argv?.toUniverse(alias, yutori),
        button = button?.toUniverse(alias, yutori),
        channel = channel?.toUniverse(alias, yutori),
        guild = guild?.toUniverse(alias, yutori),
        login = login?.toUniverse(alias, yutori),
        member = member?.toUniverse(alias, yutori),
        message = message?.toUniverse(alias, yutori),
        operator = operator?.toUniverse(alias, yutori),
        role = role?.toUniverse(alias, yutori),
        user = user?.toUniverse(alias, yutori),
    )
}

sealed class SerializableInteraction {
    @Serializable
    data class Argv
        @OptIn(ExperimentalSerializationApi::class)
        constructor(
            val name: String,
            val arguments: List<
                @Serializable(DynamicLookupSerializer::class)
                Any,
            >,
            @Serializable(DynamicLookupSerializer::class) val options: Any,
        ) : SerializableInteraction(),
            Convertable<Interaction.Argv> {
            override fun toUniverse(
                alias: String?,
                yutori: Yutori,
            ) = Interaction.Argv(
                name = name,
                arguments = arguments,
                options = options,
            )
        }

    @Serializable
    data class Button(
        val id: String,
    ) : SerializableInteraction(),
        Convertable<Interaction.Button> {
        override fun toUniverse(
            alias: String?,
            yutori: Yutori,
        ) = Interaction.Button(id = id)
    }
}

@Serializable
data class SerializableChannel(
    val id: String,
    val type: Int,
    val name: String? = null,
    @SerialName("parent_id") val parentId: String? = null,
) : Convertable<Channel> {
    override fun toUniverse(
        alias: String?,
        yutori: Yutori,
    ) = Channel(
        id = id,
        type = type,
        name = name,
        parentId = parentId,
    )
}

@Serializable
data class SerializableGuild(
    val id: String,
    val name: String? = null,
    val avatar: String? = null,
) : Convertable<Guild> {
    override fun toUniverse(
        alias: String?,
        yutori: Yutori,
    ) = Guild(
        id = id,
        name = name,
        avatar = avatar,
    )
}

@Serializable
data class SerializableLogin(
    val adapter: String,
    val platform: String? = null,
    val user: SerializableUser? = null,
    val status: Int? = null,
    val features: List<String> = listOf(),
    @SerialName("proxy_urls") val proxyUrls: List<String> = listOf(),
) : Convertable<Login> {
    override fun toUniverse(
        alias: String?,
        yutori: Yutori,
    ) = Login(
        adapter = adapter,
        platform = platform,
        user = user?.toUniverse(alias, yutori),
        status = status,
        features = features,
        proxyUrls = proxyUrls,
    )
}

@Serializable
data class SerializableGuildMember(
    val user: SerializableUser? = null,
    val nick: String? = null,
    val avatar: String? = null,
    @SerialName("joined_at") val joinedAt: Long? = null,
) : Convertable<GuildMember> {
    override fun toUniverse(
        alias: String?,
        yutori: Yutori,
    ) = GuildMember(
        user = user?.toUniverse(alias, yutori),
        nick = nick,
        avatar = avatar,
        joinedAt = joinedAt,
    )
}

@Serializable
data class SerializableMessage(
    val id: String,
    val content: String,
    val channel: SerializableChannel? = null,
    val guild: SerializableGuild? = null,
    val member: SerializableGuildMember? = null,
    val user: SerializableUser? = null,
    @SerialName("created_at") val createdAt: Long? = null,
    @SerialName("updated_at") val updatedAt: Long? = null,
) : Convertable<Message> {
    override fun toUniverse(
        alias: String?,
        yutori: Yutori,
    ) = Message(
        id = id,
        content = content.deserialize(yutori),
        channel = channel?.toUniverse(alias, yutori),
        guild = guild?.toUniverse(alias, yutori),
        member = member?.toUniverse(alias, yutori),
        user = user?.toUniverse(alias, yutori),
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

@Serializable
data class SerializableUser(
    val id: String,
    val name: String? = null,
    val nick: String? = null,
    val avatar: String? = null,
    @SerialName("is_bot") val isBot: Boolean? = null,
) : Convertable<User> {
    override fun toUniverse(
        alias: String?,
        yutori: Yutori,
    ) = User(
        id = id,
        name = name,
        nick = nick,
        avatar = avatar,
        isBot = isBot,
    )
}

@Serializable
data class SerializableGuildRole(
    val id: String,
    val name: String? = null,
) : Convertable<GuildRole> {
    override fun toUniverse(
        alias: String?,
        yutori: Yutori,
    ) = GuildRole(
        id = id,
        name = name,
    )
}

@Serializable
data class SerializablePagingList<T, U>(
    val data: List<T>,
    val next: String? = null,
) : Convertable<PagingList<U>> {
    override fun toUniverse(
        alias: String?,
        yutori: Yutori,
    ) = PagingList(
        data =
            data.map {
                if (it is Convertable<*>) {
                    it.toUniverse(alias, yutori)
                } else {
                    it
                } as U
            },
        next = next,
    )
}

@Serializable
data class SerializableBidiPagingList<T, U>(
    val data: List<T>,
    val prev: String? = null,
    val next: String? = null,
) : Convertable<BidiPagingList<U>> {
    override fun toUniverse(
        alias: String?,
        yutori: Yutori,
    ) = BidiPagingList(
        data =
            data.map {
                if (it is Convertable<*>) {
                    it.toUniverse(alias, yutori)
                } else {
                    it
                } as U
            },
        prev = prev,
        next = next,
    )
}