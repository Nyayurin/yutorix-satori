@file:Suppress("UNCHECKED_CAST")

package cn.yurn.yutori.module.satori

import cn.yurn.yutori.BidiPagingList
import cn.yurn.yutori.Channel
import cn.yurn.yutori.Event
import cn.yurn.yutori.Guild
import cn.yurn.yutori.GuildMember
import cn.yurn.yutori.GuildRole
import cn.yurn.yutori.Interaction
import cn.yurn.yutori.Login
import cn.yurn.yutori.Message
import cn.yurn.yutori.PagingList
import cn.yurn.yutori.SigningEvent
import cn.yurn.yutori.User
import cn.yurn.yutori.Yutori
import cn.yurn.yutori.argv
import cn.yurn.yutori.button
import cn.yurn.yutori.channel
import cn.yurn.yutori.guild
import cn.yurn.yutori.login
import cn.yurn.yutori.member
import cn.yurn.yutori.message
import cn.yurn.yutori.operator
import cn.yurn.yutori.role
import cn.yurn.yutori.user
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
    fun toUniverse(yutori: Yutori): U
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
    override fun toUniverse(yutori: Yutori) = Event<SigningEvent>(
        id = id,
        type = type,
        platform = platform,
        selfId = selfId,
        timestamp = timestamp,
        argv = argv?.toUniverse(yutori),
        button = button?.toUniverse(yutori),
        channel = channel?.toUniverse(yutori),
        guild = guild?.toUniverse(yutori),
        login = login?.toUniverse(yutori),
        member = member?.toUniverse(yutori),
        message = message?.toUniverse(yutori),
        operator = operator?.toUniverse(yutori),
        role = role?.toUniverse(yutori),
        user = user?.toUniverse(yutori)
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
        override fun toUniverse(yutori: Yutori) = Interaction.Argv(
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
        override fun toUniverse(yutori: Yutori) = Interaction.Button(id = id)

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
    override fun toUniverse(yutori: Yutori) = Channel(
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
    override fun toUniverse(yutori: Yutori) = Guild(
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
    override fun toUniverse(yutori: Yutori) = Login(
        adapter = adapter,
        platform = platform,
        user = user?.toUniverse(yutori),
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
    override fun toUniverse(yutori: Yutori) = GuildMember(
        user = user?.toUniverse(yutori),
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
    override fun toUniverse(yutori: Yutori) = Message(
        id = id,
        content = content.deserialize(yutori),
        channel = channel?.toUniverse(yutori),
        guild = guild?.toUniverse(yutori),
        member = member?.toUniverse(yutori),
        user = user?.toUniverse(yutori),
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
    override fun toUniverse(yutori: Yutori) = User(
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
    override fun toUniverse(yutori: Yutori) = GuildRole(
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
    override fun toUniverse(yutori: Yutori) = PagingList(
        data = data.map {
            if (it is Convertable<*>) {
                it.toUniverse(yutori)
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

    override fun toUniverse(yutori: Yutori) = BidiPagingList(
        data = data.map {
            if (it is Convertable<*>) {
                it.toUniverse(yutori)
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