package cn.yurin.yutorix.module.satori

import kotlinx.serialization.ContextualSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer

object SignalSerializer : JsonContentPolymorphicSerializer<Signal>(Signal::class) {
    override fun selectDeserializer(element: JsonElement) =
        when (val op = element.jsonObject["op"]!!.jsonPrimitive.int) {
            Signal.EVENT -> EventSignal.serializer()
            Signal.PING -> PingSignal.serializer()
            Signal.PONG -> PongSignal.serializer()
            Signal.IDENTIFY -> IdentifySignal.serializer()
            Signal.READY -> ReadySignal.serializer()
            else -> throw RuntimeException("Unknown event op: $op")
        }
}

@ExperimentalSerializationApi
object DynamicLookupSerializer : KSerializer<Any> {
    override val descriptor = ContextualSerializer(Any::class, null, emptyArray()).descriptor

    @OptIn(InternalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: Any) {
        val actualSerializer =
            encoder.serializersModule.getContextual(value::class) ?: value::class.serializer()
        @Suppress("UNCHECKED_CAST")
        encoder.encodeSerializableValue(actualSerializer as KSerializer<Any>, value)
    }

    override fun deserialize(decoder: Decoder): Any {
        error("Unsupported")
    }
}