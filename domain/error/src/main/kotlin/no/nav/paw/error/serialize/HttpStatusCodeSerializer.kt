package no.nav.paw.error.serialize

import io.ktor.http.HttpStatusCode
import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueSerializer

class HttpStatusCodeSerializer : ValueSerializer<HttpStatusCode>() {
    override fun serialize(value: HttpStatusCode, generator: JsonGenerator, provider: SerializationContext) {
        generator.writeNumber(value.value)
    }
}
