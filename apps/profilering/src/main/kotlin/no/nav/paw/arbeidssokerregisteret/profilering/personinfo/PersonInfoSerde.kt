package no.nav.paw.arbeidssokerregisteret.profilering.personinfo

import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.MapperFeature
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.cfg.EnumFeature
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.module.kotlin.KotlinFeature
import tools.jackson.module.kotlin.jacksonMapperBuilder
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer

class PersonInfoTopicSerde: Serde<PersonInfoTopic> {
    private val objectMapper = jacksonMapperBuilder {
            withReflectionCacheSize(512)
            configure(KotlinFeature.NullToEmptyCollection, true)
            configure(KotlinFeature.NullToEmptyMap, true)
            configure(KotlinFeature.NullIsSameAsDefault, false)
            configure(KotlinFeature.SingletonSupport, false)
            configure(KotlinFeature.StrictNullChecks, false)
        }
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
        .enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, DateTimeFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
        .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
        .disable(EnumFeature.READ_ENUMS_USING_TO_STRING, EnumFeature.WRITE_ENUMS_USING_TO_STRING)
        .build()

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
