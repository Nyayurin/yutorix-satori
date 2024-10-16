@file:Suppress("UNCHECKED_CAST")

package cn.yurin.yutorix.module.satori

import cn.yurin.yutori.BidiPagingList
import cn.yurin.yutori.Channel
import cn.yurin.yutori.Event
import cn.yurin.yutori.Guild
import cn.yurin.yutori.GuildMember
import cn.yurin.yutori.GuildRole
import cn.yurin.yutori.Interaction
import cn.yurin.yutori.Login
import cn.yurin.yutori.Message
import cn.yurin.yutori.PagingList
import cn.yurin.yutori.SigningEvent
import cn.yurin.yutori.User
import cn.yurin.yutori.Yutori
import cn.yurin.yutori.argv
import cn.yurin.yutori.button
import cn.yurin.yutori.channel
import cn.yurin.yutori.guild
import cn.yurin.yutori.login
import cn.yurin.yutori.member
import cn.yurin.yutori.message
import cn.yurin.yutori.operator
import cn.yurin.yutori.role
import cn.yurin.yutori.user
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
data class EventSignal(val body: SerializableEvent) : Signal() {
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
data class IdentifySignal(val body: Identify) : Signal() {
    override val op: Int

    init {
        op = IDENTIFY
    }
}

@Serializable
data class ReadySignal(val body: Ready) : Signal() {
    override val op: Int

    init {
        op = READY
    }
}

@Serializable
data class Identify(
    val token: String? = null,
    val sequence: Int? = null
)

@Serializable
data class Ready(val logins: List<SerializableLogin>)

data class SatoriAdapterProperties(
    val host: String = "127.0.0.1",
    val port: Int = 8080,
    val path: String = "",
    val token: String? = null,
    val version: String = "v1"
)

data class SatoriServerProperties(
    val listen: String = "0.0.0.0",
    val port: Int = 8080,
    val path: String = "",
    val token: String? = null,
    val version: String = "v1"
)

interface Convertable<U> {
    fun toUniverse(alias: String?, yutori: Yutori): U
}

@Serializable
class SerializableEvent(
    val id: Int,
    val type: String,
    val platform: String,
    @SerialName("self_id")
    val selfId: String,
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
    val user: SerializableUser? = null
) : Convertable<Event<SigningEvent>> {
    override fun toUniverse(alias: String?, yutori: Yutori) = Event<SigningEvent>(
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
        user = user?.toUniverse(alias, yutori)
    )

    companion object {
        fun fromUniverse(universe: Event<SigningEvent>) = SerializableEvent(
            id = universe.id.toInt(),
            type = universe.type,
            platform = universe.platform,
            selfId = universe.selfId,
            timestamp = universe.timestamp.toLong(),
            argv = universe.argv?.let { SerializableInteraction.Argv.fromUniverse(it) },
            button = universe.button?.let { SerializableInteraction.Button.fromUniverse(it) },
            channel = universe.channel?.let { SerializableChannel.fromUniverse(it) },
            guild = universe.guild?.let { SerializableGuild.fromUniverse(it) },
            login = universe.login?.let { SerializableLogin.fromUniverse(it) },
            member = universe.member?.let { SerializableGuildMember.fromUniverse(it) },
            message = universe.message?.let { SerializableMessage.fromUniverse(it) },
            operator = universe.operator?.let { SerializableUser.fromUniverse(it) },
            role = universe.role?.let { SerializableGuildRole.fromUniverse(it) },
            user = universe.user?.let { SerializableUser.fromUniverse(it) }
        )
    }
}

sealed class SerializableInteraction {
    @Serializable
    data class Argv @OptIn(ExperimentalSerializationApi::class) constructor(
        val name: String,
        val arguments: List<@Serializable(DynamicLookupSerializer::class) Any>,
        @Serializable(DynamicLookupSerializer::class)
        val options: Any
    ) : SerializableInteraction(), Convertable<Interaction.Argv> {
        override fun toUniverse(alias: String?, yutori: Yutori) = Interaction.Argv(
            name = name,
            arguments = arguments,
            options = options
        )

        companion object {
            fun fromUniverse(universe: Interaction.Argv) = Argv(
                name = universe.name,
                arguments = universe.arguments,
                options = universe.options
            )
        }
    }

    @Serializable
    data class Button(val id: String) : SerializableInteraction(), Convertable<Interaction.Button> {
        override fun toUniverse(alias: String?, yutori: Yutori) = Interaction.Button(id = id)

        companion object {
            fun fromUniverse(universe: Interaction.Button) = Button(id = universe.id)
        }
    }
}

@Serializable
data class SerializableChannel(
    val id: String,
    val type: Int,
    val name: String? = null,
    @SerialName("parent_id")
    val parentId: String? = null
) : Convertable<Channel> {
    override fun toUniverse(alias: String?, yutori: Yutori) = Channel(
        id = id,
        type = type,
        name = name,
        parentId = parentId
    )

    companion object {
        fun fromUniverse(universe: Channel) = SerializableChannel(
            id = universe.id,
            type = universe.type.toInt(),
            name = universe.name,
            parentId = universe.parentId
        )
    }
}

@Serializable
data class SerializableGuild(
    val id: String,
    val name: String? = null,
    val avatar: String? = null
) : Convertable<Guild> {
    override fun toUniverse(alias: String?, yutori: Yutori) = Guild(
        id = id,
        name = name,
        avatar = avatar
    )

    companion object {
        fun fromUniverse(universe: Guild) = SerializableGuild(
            id = universe.id,
            name = universe.name,
            avatar = universe.avatar
        )
    }
}

@Serializable
data class SerializableLogin(
    val adapter: String,
    val platform: String? = null,
    val user: SerializableUser? = null,
    val status: Int? = null,
    val features: List<String> = listOf(),
    @SerialName("proxy_urls")
    val proxyUrls: List<String> = listOf(),
) : Convertable<Login> {
    override fun toUniverse(alias: String?, yutori: Yutori) = Login(
        adapter = adapter,
        platform = platform,
        user = user?.toUniverse(alias, yutori),
        status = status,
        features = features,
        proxyUrls = proxyUrls
    )

    companion object {
        fun fromUniverse(universe: Login) = SerializableLogin(
            adapter = universe.adapter,
            platform = universe.platform,
            user = universe.user?.let { SerializableUser.fromUniverse(it) },
            status = universe.status?.toInt(),
            features = universe.features,
            proxyUrls = universe.proxyUrls
        )
    }
}

@Serializable
data class SerializableGuildMember(
    val user: SerializableUser? = null,
    val nick: String? = null,
    val avatar: String? = null,
    @SerialName("joined_at")
    val joinedAt: Long? = null
) : Convertable<GuildMember> {
    override fun toUniverse(alias: String?, yutori: Yutori) = GuildMember(
        user = user?.toUniverse(alias, yutori),
        nick = nick,
        avatar = avatar,
        joinedAt = joinedAt
    )

    companion object {
        fun fromUniverse(universe: GuildMember) = SerializableGuildMember(
            user = universe.user?.let { SerializableUser.fromUniverse(it) },
            nick = universe.nick,
            avatar = universe.avatar,
            joinedAt = universe.joinedAt?.toLong()
        )
    }
}

@Serializable
data class SerializableMessage(
    val id: String,
    val content: String,
    val channel: SerializableChannel? = null,
    val guild: SerializableGuild? = null,
    val member: SerializableGuildMember? = null,
    val user: SerializableUser? = null,
    @SerialName("created_at")
    val createdAt: Long? = null,
    @SerialName("updated_at")
    val updatedAt: Long? = null
) : Convertable<Message> {
    override fun toUniverse(alias: String?, yutori: Yutori) = Message(
        id = id,
        content = content.deserialize(yutori),
        channel = channel?.toUniverse(alias, yutori),
        guild = guild?.toUniverse(alias, yutori),
        member = member?.toUniverse(alias, yutori),
        user = user?.toUniverse(alias, yutori),
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromUniverse(universe: Message) = SerializableMessage(
            id = universe.id,
            content = universe.content.serialize(),
            channel = universe.channel?.let { SerializableChannel.fromUniverse(it) },
            guild = universe.guild?.let { SerializableGuild.fromUniverse(it) },
            member = universe.member?.let { SerializableGuildMember.fromUniverse(it) },
            user = universe.user?.let { SerializableUser.fromUniverse(it) },
            createdAt = universe.createdAt?.toLong(),
            updatedAt = universe.updatedAt?.toLong()
        )
    }
}

@Serializable
data class SerializableUser(
    val id: String,
    val name: String? = null,
    val nick: String? = null,
    val avatar: String? = null,
    @SerialName("is_bot")
    val isBot: Boolean? = null
) : Convertable<User> {
    override fun toUniverse(alias: String?, yutori: Yutori) = User(
        id = id,
        name = name,
        nick = nick,
        avatar = avatar,
        isBot = isBot
    )

    companion object {
        fun fromUniverse(universe: User) = SerializableUser(
            id = universe.id,
            name = universe.name,
            nick = universe.nick,
            avatar = universe.avatar,
            isBot = universe.isBot
        )
    }
}

@Serializable
data class SerializableGuildRole(
    val id: String,
    val name: String? = null
) : Convertable<GuildRole> {
    override fun toUniverse(alias: String?, yutori: Yutori) = GuildRole(
        id = id,
        name = name
    )

    companion object {
        fun fromUniverse(universe: GuildRole) = SerializableGuildRole(
            id = universe.id,
            name = universe.name
        )
    }
}

@Serializable
data class SerializablePagingList<T, U>(
    val data: List<T>,
    val next: String? = null
) : Convertable<PagingList<U>> {
    override fun toUniverse(alias: String?, yutori: Yutori) = PagingList(
        data = data.map {
            if (it is Convertable<*>) {
                it.toUniverse(alias, yutori)
            } else {
                it
            } as U
        },
        next = next
    )

    companion object {
        fun <T> fromUniverse(universe: PagingList<T>) = SerializablePagingList<T, T>(
            data = universe.data,
            next = universe.next
        )
    }
}

@Serializable
data class SerializableBidiPagingList<T, U>(
    val data: List<T>,
    val prev: String? = null,
    val next: String? = null
) : Convertable<BidiPagingList<U>> {
    @Serializable
    enum class Direction(val value: String) {
        Before("before"), After("after"), Around("around");

        override fun toString() = value
    }

    @Serializable
    enum class Order(val value: String) {
        Asc("asc"), Desc("desc");

        override fun toString() = value
    }

    override fun toUniverse(alias: String?, yutori: Yutori) = BidiPagingList(
        data = data.map {
            if (it is Convertable<*>) {
                it.toUniverse(alias, yutori)
            } else {
                it
            } as U
        },
        prev = prev,
        next = next
    )

    companion object {
        fun <T> fromUniverse(universe: BidiPagingList<T>) = SerializableBidiPagingList<T, T>(
            data = universe.data,
            prev = universe.prev,
            next = universe.next
        )
    }
}