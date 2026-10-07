package no.nav.paw.serialization.kontrakt

import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.KotlinInvalidNullException
import tools.jackson.module.kotlin.readValue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.client.factory.createObjectMapper
import no.nav.paw.serialization.jackson.buildObjectMapper

/**
 * Låser den generelle oppførselen til de delte ObjectMapper-oppsettene:
 * server-mapperen i `lib/serialization` ([buildObjectMapper]/`configureJackson`) og
 * klient-mapperen i `lib/http-client-utils` ([createObjectMapper]).
 * Fanger opp drift i formatet ved oppgradering av Jackson.
 *
 * Fasitfilene under `json-kontrakt/mapper/` er generert fra produksjonskoden og må aldri endres.
 */
class ObjectMapperJsonKontraktTest : FreeSpec({

    val mappere: List<Pair<String, () -> ObjectMapper>> = listOf(
        "server" to { buildObjectMapper },
        "klient" to { createObjectMapper() },
    )

    mappere.forEach { (navn, mapper) ->
        "$navn-mapper" - {
            "serialiserer eksempelobjekt likt fasit" {
                mapper().writeValueAsString(MAPPER_EKSEMPEL) skalVaereLikFasit
                    lesFasit("mapper/$navn-mapper-eksempel.json")
            }

            "deserialiserer fasit til likt eksempelobjekt" {
                mapper().readValue<MapperEksempel>(lesFasit("mapper/$navn-mapper-eksempel.json")) shouldBe
                    MAPPER_EKSEMPEL
            }

            "ignorerer ukjente felt" {
                mapper().readValue<NestetEksempel>("""{"navn":"x","antall":1,"ukjent":2}""") shouldBe
                    NestetEksempel("x", 1)
            }

            "bruker standardverdi når feltet mangler" {
                mapper().readValue<MedStandardverdi>("{}") shouldBe MedStandardverdi("standard")
            }

            /*
             * Dokumenterer dagens oppførsel: kotlinModule { ... } i configureJackson bygger en modul
             * som aldri registreres. NullToEmptyCollection/NullToEmptyMap/NullIsSameAsDefault
             * har derfor ingen effekt, og null eller manglende verdi for ikke-nullbare felt feiler.
             */
            "null for ikke-nullbar liste feiler (NullToEmptyCollection er ikke aktiv)" {
                shouldThrow<KotlinInvalidNullException> { mapper().readValue<MedListe>("""{"liste":null}""") }
            }

            "manglende ikke-nullbar liste feiler (NullToEmptyCollection er ikke aktiv)" {
                shouldThrow<KotlinInvalidNullException> { mapper().readValue<MedListe>("{}") }
            }

            "null for ikke-nullbart map feiler (NullToEmptyMap er ikke aktiv)" {
                shouldThrow<KotlinInvalidNullException> { mapper().readValue<MedMap>("""{"kart":null}""") }
            }

            "null for felt med standardverdi feiler (NullIsSameAsDefault er av)" {
                shouldThrow<KotlinInvalidNullException> {
                    mapper().readValue<MedStandardverdi>("""{"verdi":null}""")
                }
            }
        }
    }
})
