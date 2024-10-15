@file:Suppress("unused")

package cn.yurin.yutorix.module.satori

import cn.yurin.yutori.Yutori
import cn.yurin.yutori.message.element.MessageElement
import cn.yurin.yutori.message.element.Text
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Comment
import com.fleeksoft.ksoup.nodes.DocumentType
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.Node
import com.fleeksoft.ksoup.nodes.TextNode

fun String.encode() = replace("&", "&amp;")
    .replace("\"", "&quot;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")

fun String.decode() = replace("&gt;", ">")
    .replace("&lt;", "<")
    .replace("&quot;", "\"")
    .replace("&amp;", "&")

fun String.deserialize(yutori: Yutori): List<MessageElement> {
    val nodes = Ksoup.parse(this).body().childNodes().filter {
        it !is Comment && it !is DocumentType
    }
    return List(nodes.size) { i -> parseElement(yutori, nodes[i]) }
}

private fun parseElement(yutori: Yutori, node: Node): MessageElement = when (node) {
    is TextNode -> Text(node.text())
    is Element -> {
        val container = yutori.elements[when (val name = node.tagName()) {
            "a" -> "href"
            "img" -> "image"
            "b" -> "bold"
            "i" -> "idiomatic"
            "u" -> "underline"
            "s" -> "strikethrough"
            "del" -> "delete"
            "p" -> "paragraph"
            else -> name
        }]
        val properties = buildMap {
            for ((key, value) in node.attributes()) {
                put(key, value)
            }
        }.toMutableMap()
        val children = buildList {
            for (child in node.childNodes()) {
                add(parseElement(yutori, child))
            }
        }
        container?.invoke(properties, children) ?: MessageElement(
            node.tagName(),
            properties,
            children
        )
    }

    else -> throw RuntimeException("Message element parse failed: $node")
}

fun List<MessageElement>.serialize() = joinToString("") { it.serialize() }

private fun MessageElement.serialize() = buildString {
    if (this@serialize is Text) {
        append(content.encode())
        return@buildString
    }
    val nodeName = when (elementName) {
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
            }
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