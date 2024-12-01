@file:Suppress("unused")

package cn.yurin.yutorix.module.satori

import cn.yurin.yutori.*
import cn.yurin.yutori.message.element.MessageElement
import cn.yurin.yutori.message.element.Text
import cn.yurin.yutorix.module.satori.SerializableInteraction.Argv
import cn.yurin.yutorix.module.satori.SerializableInteraction.Button
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Comment
import com.fleeksoft.ksoup.nodes.DocumentType
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.Node
import com.fleeksoft.ksoup.nodes.TextNode

fun String.encode() = replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;")

fun String.decode() = replace("&gt;", ">").replace("&lt;", "<").replace("&quot;", "\"").replace("&amp;", "&")

fun String.deserialize(yutori: Yutori): List<MessageElement> {
    val nodes =
        Ksoup.parse(this).body().childNodes().filter {
            it !is Comment && it !is DocumentType
        }
    return List(nodes.size) { i -> parseElement(yutori, nodes[i]) }
}

private fun parseElement(
    yutori: Yutori,
    node: Node,
): MessageElement =
    when (node) {
        is TextNode -> Text(node.text())
        is Element -> {
            val container =
                yutori.elements[
                    when (val name = node.tagName()) {
                        "a" -> "href"
                        "img" -> "image"
                        "b" -> "bold"
                        "i" -> "idiomatic"
                        "u" -> "underline"
                        "s" -> "strikethrough"
                        "del" -> "delete"
                        "p" -> "paragraph"
                        else -> name
                    },
                ]
            val properties =
                buildMap {
                    for ((key, value) in node.attributes()) {
                        put(key, value)
                    }
                }.toMutableMap()
            val children =
                buildList {
                    for (child in node.childNodes()) {
                        add(parseElement(yutori, child))
                    }
                }
            container?.invoke(properties, children) ?: MessageElement(
                node.tagName(),
                properties,
                children,
            )
        }

        else -> throw RuntimeException("Message element parse failed: $node")
    }

fun List<MessageElement>.serialize() = joinToString("") { it.serialize() }

private fun MessageElement.serialize() =
    buildString {
        if (this@serialize is Text) {
            append(content.encode())
            return@buildString
        }
        val nodeName =
            when (elementName) {
                "href" -> "a"
                "image" -> "img"
                "bold" -> "b"
                "idiomatic" -> "i"
                "underline" -> "u"
                "strikethrough" -> "s"
                "delete" -> "del"
                "paragraph" -> "p"
                else -> elementName
            }
        append("<$nodeName")
        for (item in properties) {
            val key = item.key
            val value = item.value ?: continue
            append(" ").append(
                when (value) {
                    is String -> "$key=\"${value.encode()}\""
                    is Number -> "$key=\"$value\""
                    is Boolean -> if (value) key else ""
                    else -> throw Exception("Invalid type")
                },
            )
        }
        if (children.isEmpty()) {
            append("/>")
        } else {
            append(">")
            for (item in children) append(item)
            append("</$nodeName>")
        }
    }

fun Event<SigningEvent>.toSerializable() = SerializableEvent(
    id = id.toInt(),
    type = type,
    platform = platform,
    selfId = selfId,
    timestamp = timestamp.toLong(),
    argv = argv?.toSerializable(),
    button = button?.toSerializable(),
    channel = channel?.toSerializable(),
    guild = guild?.toSerializable(),
    login = login?.toSerializable(),
    member = member?.toSerializable(),
    message = message?.toSerializable(),
    operator = operator?.toSerializable(),
    role = role?.toSerializable(),
    user = user?.toSerializable(),
)

fun Interaction.Argv.toSerializable() = Argv(
    name = name,
    arguments = arguments,
    options = options,
)

fun Interaction.Button.toSerializable() = Button(id = id)

fun Channel.toSerializable() = SerializableChannel(
    id = id,
    type = type.toInt(),
    name = name,
    parentId = parentId,
)

fun Guild.toSerializable() = SerializableGuild(
    id = id,
    name = name,
    avatar = avatar,
)

fun Login.toSerializable() = SerializableLogin(
    adapter = adapter,
    platform = platform,
    user = user?.toSerializable(),
    status = status?.toInt(),
    features = features,
    proxyUrls = proxyUrls,
)

fun GuildMember.toSerializable() = SerializableGuildMember(
    user = user?.toSerializable(),
    nick = nick,
    avatar = avatar,
    joinedAt = joinedAt?.toLong(),
)

fun Message.toSerializable() = SerializableMessage(
    id = id,
    content = content.serialize(),
    channel = channel?.toSerializable(),
    guild = guild?.toSerializable(),
    member = member?.toSerializable(),
    user = user?.toSerializable(),
    createdAt = createdAt?.toLong(),
    updatedAt = updatedAt?.toLong(),
)

fun User.toSerializable() = SerializableUser(
    id = id,
    name = name,
    nick = nick,
    avatar = avatar,
    isBot = isBot,
)

fun GuildRole.toSerializable() = SerializableGuildRole(
    id = id,
    name = name,
)

fun <T> PagingList<T>.toSerializable() = SerializablePagingList<T, T>(
    data = data,
    next = next,
)

fun <T> BidiPagingList<T>.toSerializable() = SerializableBidiPagingList<T, T>(
    data = data,
    prev = prev,
    next = next,
)