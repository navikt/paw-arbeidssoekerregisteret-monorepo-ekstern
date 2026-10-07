package no.nav.paw.arbeidssokerregisteret.profilering.kontrakt

import io.kotest.assertions.json.ArrayOrder
import io.kotest.assertions.json.FieldComparison
import io.kotest.assertions.json.NumberFormat
import io.kotest.assertions.json.PropertyOrder
import io.kotest.assertions.json.TypeCoercion
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.style.FreeSpec
import io.kotest.core.spec.style.scopes.FreeSpecContainerScope
import io.kotest.matchers.shouldBe
import no.nav.paw.aareg.model.Ansettelsesdetaljer
import no.nav.paw.aareg.model.Ansettelsesperiode
import no.nav.paw.aareg.model.Arbeidsforhold
import no.nav.paw.aareg.model.Arbeidssted
import no.nav.paw.aareg.model.Ident
import no.nav.paw.aareg.model.Opplysningspliktig
import no.nav.paw.arbeidssokerregisteret.profilering.personinfo.PersonInfo
import no.nav.paw.arbeidssokerregisteret.profilering.personinfo.PersonInfoTopic
import no.nav.paw.arbeidssokerregisteret.profilering.personinfo.PersonInfoTopicSerde
import org.apache.kafka.common.serialization.Serde
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/**
 * JSON-kontrakttester (fasitfiler) for [PersonInfoTopic], som produseres til Kafka via
 * produksjonens [PersonInfoTopicSerde]. Arbeidsforhold kommer fra lib/aareg-client.
 *
 * Merk: serden slår ikke av WRITE_DATES_AS_TIMESTAMPS, så LocalDate og LocalDateTime skrives i dag
 * som tall-arrayer (f.eks. [2014,1,1]). Det er låst her slik det er.
 *
 * Fasitfilene under `src/test/resources/json-kontrakt/` må aldri endres. Ved formatendring legges
 * nytt format i en ny fil, og de gamle filene beholdes.
 */
class PersonInfoTopicJsonKontraktTest : FreeSpec({
    val serde = PersonInfoTopicSerde()
    val profileringId = UUID.fromString("9c1f6a2e-7d3b-4e5f-a6b7-c8d9e0f1a2b3")

    "PersonInfoTopic til Kafka" - {
        "fullt utfylt med arbeidsforhold" - {
            sjekkKontrakt(
                serde = serde,
                fil = "json-kontrakt/personinfo-topic/personinfo-fullt-utfylt.json",
                eksempel = PersonInfoTopic(
                    profileringId = profileringId,
                    personInfo = PersonInfo(
                        foedselsdato = LocalDate.of(1990, 2, 27),
                        foedselsAar = 1990,
                        arbeidsforhold = listOf(
                            Arbeidsforhold(
                                arbeidssted = Arbeidssted(
                                    type = "Underenhet",
                                    identer = listOf(Ident(type = "ORGANISASJONSNUMMER", ident = "910825518"))
                                ),
                                ansettelsesdetaljer = listOf(
                                    Ansettelsesdetaljer(type = "Ordinaer", avtaltStillingsprosent = 100.0),
                                    Ansettelsesdetaljer(type = "Maritim", avtaltStillingsprosent = 37.5)
                                ),
                                opplysningspliktig = Opplysningspliktig(
                                    type = "Hovedenhet",
                                    identer = listOf(Ident(type = "ORGANISASJONSNUMMER", ident = "810825472"))
                                ),
                                ansettelsesperiode = Ansettelsesperiode(
                                    startdato = LocalDate.of(2014, 1, 1),
                                    sluttdato = LocalDate.of(2025, 12, 31)
                                ),
                                opprettet = LocalDateTime.of(2020, 1, 23, 9, 5, 7, 123456000)
                            ),
                            Arbeidsforhold(
                                arbeidssted = Arbeidssted(
                                    type = "Person",
                                    identer = listOf(Ident(type = "AKTORID", ident = "2175141353812"))
                                ),
                                ansettelsesdetaljer = emptyList(),
                                opplysningspliktig = Opplysningspliktig(type = "Person", identer = emptyList()),
                                ansettelsesperiode = Ansettelsesperiode(
                                    startdato = LocalDate.of(2026, 1, 1),
                                    sluttdato = null
                                ),
                                opprettet = LocalDateTime.of(2026, 1, 15, 10, 15, 30, 0)
                            )
                        )
                    )
                )
            )
        }
        "uten foedselsdato, foedselsAar og arbeidsforhold" - {
            sjekkKontrakt(
                serde = serde,
                fil = "json-kontrakt/personinfo-topic/personinfo-tomme-felt.json",
                eksempel = PersonInfoTopic(
                    profileringId = profileringId,
                    personInfo = PersonInfo(
                        foedselsdato = null,
                        foedselsAar = null,
                        arbeidsforhold = emptyList()
                    )
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
    requireNotNull(PersonInfoTopicJsonKontraktTest::class.java.classLoader.getResource(fil)) { "Fant ikke fasitfil $fil" }
        .readText(Charsets.UTF_8)
