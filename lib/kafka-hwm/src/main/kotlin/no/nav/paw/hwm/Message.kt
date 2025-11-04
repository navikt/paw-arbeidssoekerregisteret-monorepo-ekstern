package no.nav.paw.hwm

import io.opentelemetry.api.trace.Span
import org.apache.kafka.clients.consumer.ConsumerRecord
import java.time.Instant

data class Message<K, V>(
    val span: Span,
    val key: K,
    val value: V,
    val topic: String,
    val partition: Int,
    val offset: Long,
    val timestamp: Instant
)

fun <K, V> ConsumerRecord<K, V>.toMessage() = Message(
    span = Span.current(),
    key = this.key(),
    value = this.value(),
    topic = this.topic(),
    partition = this.partition(),
    offset = this.offset(),
    timestamp = Instant.ofEpochMilli(this.timestamp())
)
