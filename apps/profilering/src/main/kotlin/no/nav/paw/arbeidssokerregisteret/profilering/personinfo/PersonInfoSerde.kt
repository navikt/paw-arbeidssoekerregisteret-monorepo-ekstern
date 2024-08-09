package no.nav.paw.arbeidssokerregisteret.profilering.personinfo

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer

class PersonInfoTopicSerde: Serde<PersonInfoTopic> {
    private val objectMapper = ObjectMapper()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .registerModules(
            KotlinModule.Builder()
                .withReflectionCacheSize(512)
                .configure(KotlinFeature.NullToEmptyCollection, true)
                .configure(KotlinFeature.NullToEmptyMap, true)
                .configure(KotlinFeature.NullIsSameAsDefault, false)
                .configure(KotlinFeature.SingletonSupport, false)
                .configure(KotlinFeature.StrictNullChecks, false)
                .build(),
            com.fasterxml.jackson.datatype.jsr310.JavaTimeModule()
        )

    override fun serializer(): Serializer<PersonInfoTopic> = PersonInfoSerializer(objectMapper)

    override fun deserializer(): Deserializer<PersonInfoTopic> = PersonInfoDeserializer(objectMapper)
}

class PersonInfoSerializer(private val objectMapper: ObjectMapper): Serializer<PersonInfoTopic> {
    override fun serialize(topic: String?, data: PersonInfoTopic?): ByteArray {
        return objectMapper.writeValueAsBytes(data)
    }
}

class PersonInfoDeserializer(private val objectMapper: ObjectMapper): Deserializer<PersonInfoTopic> {
    override fun deserialize(topic: String?, data: ByteArray?): PersonInfoTopic? {
        if (data == null) return null
        return objectMapper.readValue(data, PersonInfoTopic::class.java)
    }
}