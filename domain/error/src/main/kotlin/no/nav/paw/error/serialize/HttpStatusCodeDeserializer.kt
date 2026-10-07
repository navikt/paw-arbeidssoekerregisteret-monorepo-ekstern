package no.nav.paw.error.serialize

import io.ktor.http.HttpStatusCode
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.ValueDeserializer

class HttpStatusCodeDeserializer : ValueDeserializer<HttpStatusCode>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): HttpStatusCode {
        return HttpStatusCode.fromValue(parser.numberValue.toInt())
    }
}
