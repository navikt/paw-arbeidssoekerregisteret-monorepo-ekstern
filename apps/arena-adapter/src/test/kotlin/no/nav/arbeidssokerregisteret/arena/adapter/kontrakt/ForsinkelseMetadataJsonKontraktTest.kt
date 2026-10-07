package no.nav.arbeidssokerregisteret.arena.adapter.kontrakt

import io.kotest.assertions.json.ArrayOrder
import io.kotest.assertions.json.FieldComparison
import io.kotest.assertions.json.NumberFormat
import io.kotest.assertions.json.PropertyOrder
import io.kotest.assertions.json.TypeCoercion
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.style.FreeSpec
import io.kotest.core.spec.style.scopes.FreeSpecContainerScope
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssokerregisteret.arena.adapter.ForsinkelseMetadata
import no.nav.paw.arbeidssokerregisteret.arena.adapter.forsinkelseSerde
import org.apache.kafka.common.serialization.Serde
import java.time.Instant

/**
 * JSON-kontrakttester (fasitfiler) for [ForsinkelseMetadata], som lagres i state store for ventende
 * perioder via produksjonens `forsinkelseSerde` (TopologyPunctuation.kt).
 *
 * Fasitfilene under `src/test/resources/json-kontrakt/` må aldri endres. Dette er lagret data: ved
 * formatendring legges nytt format i en ny fil, og de gamle filene beholdes slik at vi fortsatt
 * beviser at eksisterende data i state store kan leses.
 */
class ForsinkelseMetadataJsonKontraktTest : FreeSpec({
    val timestamp = Instant.parse("2026-01-15T10:15:30.123Z").toEpochMilli()

    "ForsinkelseMetadata i state store" - {
        "med traceparent" - {
            sjekkKontrakt(
                serde = forsinkelseSerde,
                fil = "json-kontrakt/forsinkelse-state-store/forsinkelse-metadata-med-traceparent.json",
                eksempel = ForsinkelseMetadata(
                    recordKey = -1234567L,
                    traceparent = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01",
                    timestamp = timestamp
                )
            )
        }
        "uten traceparent (null)" - {
            sjekkKontrakt(
                serde = forsinkelseSerde,
                fil = "json-kontrakt/forsinkelse-state-store/forsinkelse-metadata-uten-traceparent.json",
                eksempel = ForsinkelseMetadata(
                    recordKey = 7654321L,
                    traceparent = null,
                    timestamp = timestamp
                )
            )
        }
    }
})

private suspend fun <T> FreeSpecContainerScope.sjekkKontrakt(serde: Serde<T>, fil: String, eksempel: T) {
    val fasit = lesRessurs(fil)
    "serialisering gir JSON lik fasitfil" {
        val produsert = serde.serializer().serialize("topic", eksempel).toString(Charsets.UTF_8)
        produsert shouldEqualJson {
            propertyOrder = PropertyOrder.Strict
            fieldComparison = FieldComparison.Strict
            numberFormat = NumberFormat.Strict
            arrayOrder = ArrayOrder.Strict
            typeCoercion = TypeCoercion.Disabled
            fasit
        }
    }
    "deserialisering av fasitfil gir eksempelobjektet" {
        serde.deserializer().deserialize("topic", fasit.toByteArray(Charsets.UTF_8)) shouldBe eksempel
    }
}

private fun lesRessurs(fil: String): String =
    requireNotNull(ForsinkelseMetadataJsonKontraktTest::class.java.classLoader.getResource(fil)) { "Fant ikke fasitfil $fil" }
        .readText(Charsets.UTF_8)
