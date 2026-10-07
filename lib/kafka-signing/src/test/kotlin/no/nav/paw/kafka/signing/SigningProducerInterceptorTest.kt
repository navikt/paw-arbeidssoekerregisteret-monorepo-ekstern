package no.nav.paw.kafka.signing

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeaders
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.serialization.StringSerializer
import java.nio.file.Files
import java.security.KeyPairGenerator
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.util.Base64

class FailingStringSerializer : Serializer<String> {
    override fun serialize(topic: String, data: String): ByteArray = error("Serialization failed")
}

class SigningProducerInterceptorTest : FreeSpec({

    val keyPair = KeyPairGenerator.getInstance("EC").apply { initialize(256) }.generateKeyPair()
    val privateKey = keyPair.private as ECPrivateKey
    val publicKey = keyPair.public as ECPublicKey

    val pkcs8B64 = Base64.getEncoder().encodeToString(privateKey.encoded)
    val keyId = "ecdsa-v1"

    val secretDir = Files.createTempDirectory("kafka-signing-interceptor-test").toFile().also { dir ->
        dir.resolve(KAFKA_SIGNING_SECRET_KEY_PRIVATE_KEY).writeText(pkcs8B64)
        dir.resolve(KAFKA_SIGNING_SECRET_KEY_ID).writeText(keyId)
    }
    val localPropertiesFile = Files.createTempFile("kafka-signing-local", ".properties").toFile().also { f ->
        f.writeText("""
            $KAFKA_SIGNING_SECRET_KEY_PRIVATE_KEY=$pkcs8B64
            $KAFKA_SIGNING_SECRET_KEY_ID=$keyId
        """.trimIndent())
    }

    afterSpec {
        secretDir.deleteRecursively()
        localPropertiesFile.delete()
    }

    fun interceptor(valueSerializer: Class<*> = StringSerializer::class.java): SigningProducerInterceptor<String, String> {
        return SigningProducerInterceptor<String, String>().also {
            it.configure(
                mapOf(
                    SigningProducerInterceptor.PAW_SIGNING_MOUNT_PATH to secretDir.absolutePath,
                    SigningProducerInterceptor.PAW_SIGNING_LOCAL_RESOURCE to localPropertiesFile.absolutePath,
                    ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java.name,
                    ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to valueSerializer.name,
                )
            )
        }
    }

    "adds x-paw-signature and x-paw-signing-key-id headers" {
        val interceptor = interceptor()
        val record = ProducerRecord<String, String>("test-topic", "record-key", """{"hello":"world"}""")

        val signed = interceptor.onSend(record)

        signed.headers().lastHeader("x-paw-signature") shouldNotBe null
        signed.headers().lastHeader("x-paw-signing-key-id")?.value()?.toString(Charsets.UTF_8) shouldBe keyId
    }

    "replaces existing signing headers and preserves all other headers" {
        val headers = RecordHeaders().apply {
            add(SIGNATURE_HEADER, "old-signature".toByteArray())
            add("traceparent", "trace-value".toByteArray())
            add(SIGNING_KEY_ID_HEADER, "old-key-id".toByteArray())
            add("x-custom", "first".toByteArray())
            add("x-custom", "second".toByteArray())
        }
        val record = ProducerRecord("test-topic", null, 1_700_000_000_000L, "record-key", "value", headers)

        val signed = interceptor().onSend(record)

        signed.headers().headers(SIGNATURE_HEADER).count() shouldBe 1
        signed.headers().headers(SIGNING_KEY_ID_HEADER).count() shouldBe 1
        signed.headers().lastHeader(SIGNING_KEY_ID_HEADER).value().toString(Charsets.UTF_8) shouldBe keyId
        val signature = Base64.getUrlDecoder().decode(signed.headers().lastHeader(SIGNATURE_HEADER).value())
        verifyKafkaRecord("record-key".toByteArray(), "trace-value".toByteArray(), signed.timestamp()!!,
            "value".toByteArray(), signature, publicKey) shouldBe true
        signed.headers().toArray().filter { it.key() !in setOf(SIGNATURE_HEADER, SIGNING_KEY_ID_HEADER) }
            .map { it.key() to it.value().toString(Charsets.UTF_8) } shouldBe
            listOf("traceparent" to "trace-value", "x-custom" to "first", "x-custom" to "second")
        record.headers().lastHeader(SIGNATURE_HEADER).value().toString(Charsets.UTF_8) shouldBe "old-signature"
    }

    "removes existing signing headers even when signing fails and preserves other headers" {
        val headers = RecordHeaders().apply {
            add("traceparent", "trace-value".toByteArray())
            add(SIGNATURE_HEADER, "old-signature".toByteArray())
            add("x-custom", "first".toByteArray())
            add(SIGNING_KEY_ID_HEADER, "old-key-id".toByteArray())
            add("x-custom", "second".toByteArray())
        }
        val record = ProducerRecord("test-topic", null, null, "record-key", "value", headers)

        val unsigned = interceptor(FailingStringSerializer::class.java).onSend(record)

        unsigned.headers().toArray().map { it.key() to it.value().toString(Charsets.UTF_8) } shouldBe
            listOf("traceparent" to "trace-value", "x-custom" to "first", "x-custom" to "second")
        unsigned.timestamp() shouldBe null
        record.headers().lastHeader(SIGNING_KEY_ID_HEADER).value().toString(Charsets.UTF_8) shouldBe "old-key-id"
    }

    "signature is verifiable offline using public key" {
        val interceptor = interceptor()
        val record = ProducerRecord<String, String>("test-topic", null, 1_700_000_000_000L, "record-key", """{"hello":"world"}""")

        val signed = interceptor.onSend(record)

        val signatureBytes = Base64.getUrlDecoder().decode(signed.headers().lastHeader("x-paw-signature").value())
        val keyBytes = StringSerializer().serialize("test-topic", "record-key")
        val valueBytes = StringSerializer().serialize("test-topic", """{"hello":"world"}""")
        val timestamp = signed.timestamp()!!

        verifyKafkaRecord(keyBytes, ByteArray(0), timestamp, valueBytes, signatureBytes, publicKey) shouldBe true
    }

    "signature includes traceparent when present" {
        val interceptor = interceptor()
        val traceparent = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"
        val headers = RecordHeaders().apply { add("traceparent", traceparent.toByteArray()) }
        val record = ProducerRecord("test-topic", null, 1_700_000_000_000L, "record-key", """{"hello":"world"}""", headers)

        val signed = interceptor.onSend(record)

        val signatureBytes = Base64.getUrlDecoder().decode(signed.headers().lastHeader("x-paw-signature").value())
        val keyBytes = StringSerializer().serialize("test-topic", "record-key")
        val valueBytes = StringSerializer().serialize("test-topic", """{"hello":"world"}""")
        val timestamp = signed.timestamp()!!

        verifyKafkaRecord(keyBytes, traceparent.toByteArray(), timestamp, valueBytes, signatureBytes, publicKey) shouldBe true
    }

    "sets explicit timestamp when record has no timestamp" {
        val interceptor = interceptor()
        val record = ProducerRecord<String, String>("test-topic", "key", "value")

        val signed = interceptor.onSend(record)

        signed.timestamp() shouldNotBe null
    }

    "interceptor can be loaded by class name (Kafka config pattern)" {
        val clazz = Class.forName("no.nav.paw.kafka.signing.SigningProducerInterceptor")
        clazz shouldNotBe null
    }
})
