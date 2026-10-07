package no.nav.paw.oppslagapi.data.kontrakt

import io.kotest.assertions.json.ArrayOrder
import io.kotest.assertions.json.FieldComparison
import io.kotest.assertions.json.NumberFormat
import io.kotest.assertions.json.PropertyOrder
import io.kotest.assertions.json.TypeCoercion
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssokerregisteret.api.v1.AvviksType
import no.nav.paw.arbeidssokerregisteret.api.v1.Beskrivelse
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import no.nav.paw.arbeidssokerregisteret.api.v1.TidspunktFraKilde
import no.nav.paw.bekreftelse.melding.v1.vo.Bekreftelsesloesning
import no.nav.paw.oppslagapi.data.bekreftelsemelding_v1
import no.nav.paw.oppslagapi.data.consumer.toRow
import no.nav.paw.oppslagapi.data.egenvurdering_v1
import no.nav.paw.oppslagapi.data.opplysninger_om_arbeidssoeker_v4
import no.nav.paw.oppslagapi.data.pa_vegne_av_start_v1
import no.nav.paw.oppslagapi.data.pa_vegne_av_stopp_v1
import no.nav.paw.oppslagapi.data.periode_avsluttet_v1
import no.nav.paw.oppslagapi.data.periode_startet_v1
import no.nav.paw.oppslagapi.data.profilering_v1
import no.nav.paw.oppslagapi.data.typeTilKlasse
import no.nav.paw.oppslagapi.test.opprettSerde
import no.nav.paw.oppslagapi.utils.objectMapper
import no.nav.paw.test.data.bekreftelse.bekreftelseMelding
import no.nav.paw.test.data.bekreftelse.startPaaVegneAv
import no.nav.paw.test.data.bekreftelse.stoppPaaVegneAv
import no.nav.paw.test.data.periode.PeriodeFactory
import no.nav.paw.test.data.periode.createAnnet
import no.nav.paw.test.data.periode.createEgenvurdering
import no.nav.paw.test.data.periode.createHelse
import no.nav.paw.test.data.periode.createJobbsituasjon
import no.nav.paw.test.data.periode.createOpplysninger
import no.nav.paw.test.data.periode.createProfilering
import no.nav.paw.test.data.periode.createUtdanning
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.header.internals.RecordHeaders
import org.apache.kafka.common.record.TimestampType
import java.time.Duration
import java.time.Instant
import java.util.Optional
import java.util.UUID
import no.nav.paw.bekreftelse.paavegneav.v1.vo.Bekreftelsesloesning as PaaVegneAvBekreftelsesloesning
import no.nav.paw.oppslagapi.model.v2.Bekreftelse as V2Bekreftelse
import no.nav.paw.oppslagapi.model.v2.Egenvurdering as V2Egenvurdering
import no.nav.paw.oppslagapi.model.v2.Metadata as V2Metadata
import no.nav.paw.oppslagapi.model.v2.OpplysningerOmArbeidssoeker as V2Opplysninger
import no.nav.paw.oppslagapi.model.v2.PaaVegneAvStart as V2PaaVegneAvStart
import no.nav.paw.oppslagapi.model.v2.PaaVegneAvStopp as V2PaaVegneAvStopp
import no.nav.paw.oppslagapi.model.v2.Profilering as V2Profilering

/**
 * Kontrakttester for JSON-formatet som lagres i jsonb-kolonnen `data` (skrives i
 * `ConvertRecordToRow.toRow`, leses i `DatabaseQuerySupport.asObject` via `typeTilKlasse`).
 *
 * Fasitfilene under `json-kontrakt/lagret/` må aldri endres. Endres formatet, må det legges
 * til nye fasitfiler, og de gamle må beholdes, siden data lagret i gammelt format fortsatt skal kunne leses.
 */
class LagretDataJsonKontraktTest : FreeSpec({

    val serde = opprettSerde<SpecificRecord>()

    "typeTilKlasse er låst" {
        typeTilKlasse.mapValues { it.value.java } shouldBe mapOf(
            "periode_startet-v1" to V2Metadata::class.java,
            "periode_avsluttet-v1" to V2Metadata::class.java,
            "opplysninger-v4" to V2Opplysninger::class.java,
            "profilering-v1" to V2Profilering::class.java,
            "bekreftelse-v1" to V2Bekreftelse::class.java,
            "pa_vegne_av_start-v1" to V2PaaVegneAvStart::class.java,
            "pa_vegne_av_stopp-v1" to V2PaaVegneAvStopp::class.java,
            "egenvurdering-v1" to V2Egenvurdering::class.java,
        )
    }

    "Alle typer har minst én fasitfil" {
        eksempler.map { it.type }.toSet() shouldContainAll typeTilKlasse.keys
    }

    eksempler.forEach { eksempel ->
        val filnavn = "${eksempel.type}--${eksempel.navn}.json"
        val ressurs = "/json-kontrakt/lagret/$filnavn"
        "$filnavn" - {
            val row = eksempel.record.tilConsumerRecord(serde).toRow(serde.deserializer())

            "Lagres med forventet type" {
                row.type shouldBe eksempel.type
            }

            "Serialiserer til samme format som fasit" {
                row.data shouldEqualJson {
                    propertyOrder = PropertyOrder.Strict
                    fieldComparison = FieldComparison.Strict
                    numberFormat = NumberFormat.Strict
                    arrayOrder = ArrayOrder.Strict
                    typeCoercion = TypeCoercion.Disabled
                    lesRessurs(ressurs)
                }
            }

            "Kan lese fasit med dagens kode" {
                val forventet = objectMapper.readValue(row.data, typeTilKlasse.getValue(row.type).java)
                val lest = objectMapper.readValue(lesRessurs(ressurs), typeTilKlasse.getValue(eksempel.type).java)
                lest shouldBe forventet
                lest shouldBe eksempel.forventet
            }
        }
    }
})

private fun lesRessurs(sti: String): String =
    requireNotNull(LagretDataJsonKontraktTest::class.java.getResource(sti)) { "Fant ikke $sti" }.readText()

private const val KAFKA_TIMESTAMP = 1768472130123L // 2026-01-15T10:15:30.123Z

private fun SpecificRecord.tilConsumerRecord(
    serde: org.apache.kafka.common.serialization.Serde<SpecificRecord>
): ConsumerRecord<ByteArray, ByteArray> = ConsumerRecord(
    "topic", 0, 0L, KAFKA_TIMESTAMP, TimestampType.CREATE_TIME, 8, -1,
    ByteArray(8), serde.serializer().serialize("topic", this), RecordHeaders(), Optional.empty()
)

private data class Eksempel(
    val type: String,
    val navn: String,
    val record: SpecificRecord,
    val forventet: Any
)

private val periodeId = UUID.fromString("11111111-1111-1111-1111-111111111111")
private val opplysningerId = UUID.fromString("22222222-2222-2222-2222-222222222222")
private val profileringId = UUID.fromString("33333333-3333-3333-3333-333333333333")
private val egenvurderingId = UUID.fromString("44444444-4444-4444-4444-444444444444")
private val bekreftelseId = UUID.fromString("55555555-5555-5555-5555-555555555555")
private const val IDENT = "01017012345"
private val tidspunkt = Instant.parse("2026-01-15T10:15:30.123456Z")
private val tidspunktFraKilde = Instant.parse("2026-01-14T09:00:00.654321Z")
private val millis = Instant.parse("2026-01-15T10:15:30.123Z")
private val millisFraKilde = Instant.parse("2026-01-14T09:00:00.654Z")

private fun metadata(
    brukerType: BrukerType,
    sikkerhetsnivaa: String?,
    avviksType: AvviksType?
) = Metadata(
    tidspunkt,
    Bruker(brukerType, IDENT, sikkerhetsnivaa),
    "kontrakt-test",
    "testing",
    avviksType?.let { TidspunktFraKilde(tidspunktFraKilde, it) }
)

private fun v2Metadata(
    brukerType: no.nav.paw.oppslagapi.model.v2.BrukerType,
    sikkerhetsnivaa: String?,
    avviksType: no.nav.paw.oppslagapi.model.v2.AvviksType?
) = V2Metadata(
    tidspunkt = millis,
    utfoertAv = no.nav.paw.oppslagapi.model.v2.Bruker(brukerType, IDENT, sikkerhetsnivaa),
    kilde = "kontrakt-test",
    aarsak = "testing",
    tidspunktFraKilde = avviksType?.let { no.nav.paw.oppslagapi.model.v2.TidspunktFraKilde(millisFraKilde, it) }
)

private typealias V2BT = no.nav.paw.oppslagapi.model.v2.BrukerType
private typealias V2AT = no.nav.paw.oppslagapi.model.v2.AvviksType
private typealias V2PT = no.nav.paw.oppslagapi.model.v2.ProfilertTil
private typealias V2JN = no.nav.paw.oppslagapi.model.v2.JaNeiVetIkke
private typealias V2BL = no.nav.paw.oppslagapi.model.v2.Bekreftelsesloesning

private val periodeEksempler = listOf(
    Triple(BrukerType.VEILEDER, AvviksType.TIDSPUNKT_KORRIGERT, "veileder-tidspunkt_korrigert") to (V2BT.VEILEDER to V2AT.TIDSPUNKT_KORRIGERT),
    Triple(BrukerType.SYSTEM, AvviksType.FORSINKELSE, "system-forsinkelse") to (V2BT.SYSTEM to V2AT.FORSINKELSE),
    Triple(BrukerType.SLUTTBRUKER, AvviksType.SLETTET, "sluttbruker-slettet") to (V2BT.SLUTTBRUKER to V2AT.SLETTET),
    Triple(BrukerType.UKJENT_VERDI, AvviksType.RETTING, "ukjent_verdi-retting") to (V2BT.UKJENT_VERDI to V2AT.RETTING),
    Triple(BrukerType.UDEFINERT, AvviksType.UKJENT_VERDI, "udefinert-ukjent_verdi") to (V2BT.UDEFINERT to V2AT.UKJENT_VERDI),
)

private val alleBeskrivelser = Beskrivelse.entries.mapIndexed { i, b -> b to mapOf("nokkel$i" to "verdi$i") }

private val eksempler: List<Eksempel> = buildList {
    // Periode startet/avsluttet -> Metadata
    periodeEksempler.forEach { (avro, v2) ->
        val (bt, at, navn) = avro
        add(
            Eksempel(
                periode_startet_v1, navn,
                PeriodeFactory.create().build(periodeId, IDENT, metadata(bt, "tokenx:Level4", at), null),
                v2Metadata(v2.first, "tokenx:Level4", v2.second)
            )
        )
    }
    add(
        Eksempel(
            periode_startet_v1, "null-felter",
            PeriodeFactory.create().build(periodeId, IDENT, metadata(BrukerType.SLUTTBRUKER, null, null), null),
            v2Metadata(V2BT.SLUTTBRUKER, null, null)
        )
    )
    add(
        Eksempel(
            periode_avsluttet_v1, "fullt-utfylt",
            PeriodeFactory.create().build(
                periodeId, IDENT,
                metadata(BrukerType.SLUTTBRUKER, "tokenx:Level4", AvviksType.UKJENT_VERDI),
                metadata(BrukerType.SYSTEM, "tokenx:Level4", AvviksType.FORSINKELSE)
            ),
            v2Metadata(V2BT.SYSTEM, "tokenx:Level4", V2AT.FORSINKELSE)
        )
    )
    add(
        Eksempel(
            periode_avsluttet_v1, "null-felter",
            PeriodeFactory.create().build(
                periodeId, IDENT,
                metadata(BrukerType.SLUTTBRUKER, "tokenx:Level4", AvviksType.UKJENT_VERDI),
                metadata(BrukerType.VEILEDER, null, null)
            ),
            v2Metadata(V2BT.VEILEDER, null, null)
        )
    )

    // Opplysninger
    add(
        Eksempel(
            opplysninger_om_arbeidssoeker_v4, "fullt-utfylt-alle-beskrivelser",
            createOpplysninger(
                id = opplysningerId,
                periodeId = periodeId,
                helse = createHelse(true),
                annet = createAnnet(false),
                sendtInnAv = metadata(BrukerType.SLUTTBRUKER, "tokenx:Level4", AvviksType.UKJENT_VERDI),
                jobbsituasjon = createJobbsituasjon(alleBeskrivelser),
                utdanning = createUtdanning(nus = "4", godkjent = null, bestatt = true)
            ),
            V2Opplysninger(
                id = opplysningerId,
                periodeId = periodeId,
                sendtInnAv = v2Metadata(V2BT.SLUTTBRUKER, "tokenx:Level4", V2AT.UKJENT_VERDI),
                utdanning = no.nav.paw.oppslagapi.model.v2.Utdanning("4", V2JN.JA, V2JN.VET_IKKE),
                helse = no.nav.paw.oppslagapi.model.v2.Helse(V2JN.JA),
                jobbsituasjon = no.nav.paw.oppslagapi.model.v2.Jobbsituasjon(
                    alleBeskrivelser.map { (b, d) ->
                        no.nav.paw.oppslagapi.model.v2.BeskrivelseMedDetaljer(
                            no.nav.paw.oppslagapi.model.v2.Beskrivelse.valueOf(b.name), d
                        )
                    }
                ),
                annet = no.nav.paw.oppslagapi.model.v2.Annet(V2JN.NEI)
            )
        )
    )
    add(
        Eksempel(
            opplysninger_om_arbeidssoeker_v4, "null-felter",
            createOpplysninger(
                id = opplysningerId,
                periodeId = periodeId,
                helse = null,
                annet = null,
                sendtInnAv = metadata(BrukerType.VEILEDER, null, null),
                jobbsituasjon = createJobbsituasjon(emptyList()),
                utdanning = null
            ),
            V2Opplysninger(
                id = opplysningerId,
                periodeId = periodeId,
                sendtInnAv = v2Metadata(V2BT.VEILEDER, null, null),
                jobbsituasjon = no.nav.paw.oppslagapi.model.v2.Jobbsituasjon(emptyList())
            )
        )
    )

    // Profilering, en per ProfilertTil
    ProfilertTil.entries.forEachIndexed { i, pt ->
        add(
            Eksempel(
                profilering_v1, pt.name.lowercase(),
                createProfilering(
                    id = profileringId,
                    periodeId = periodeId,
                    opplysningerId = opplysningerId,
                    sendtInnAv = metadata(BrukerType.SYSTEM, null, if (i == 0) AvviksType.UKJENT_VERDI else null),
                    profilertTil = pt,
                    jobbetSammenhengende = i % 2 == 0,
                    alder = 42
                ),
                V2Profilering(
                    id = profileringId,
                    periodeId = periodeId,
                    opplysningerOmArbeidssokerId = opplysningerId,
                    sendtInnAv = v2Metadata(V2BT.SYSTEM, null, if (i == 0) V2AT.UKJENT_VERDI else null),
                    profilertTil = V2PT.valueOf(pt.name),
                    jobbetSammenhengendeSeksAvTolvSisteMnd = i % 2 == 0,
                    alder = 42
                )
            )
        )
    }

    // Egenvurdering, en per ProfilertTil
    ProfilertTil.entries.forEachIndexed { i, pt ->
        val profilertTil = ProfilertTil.entries[(i + 1) % ProfilertTil.entries.size]
        add(
            Eksempel(
                egenvurdering_v1, pt.name.lowercase(),
                createEgenvurdering(
                    id = egenvurderingId,
                    periodeId = periodeId,
                    profileringId = profileringId,
                    sendtInnAv = metadata(BrukerType.SLUTTBRUKER, if (i == 0) "tokenx:Level4" else null, null),
                    profilertTil = profilertTil,
                    egenvurdering = pt
                ),
                V2Egenvurdering(
                    id = egenvurderingId,
                    periodeId = periodeId,
                    profileringId = profileringId,
                    sendtInnAv = v2Metadata(V2BT.SLUTTBRUKER, if (i == 0) "tokenx:Level4" else null, null),
                    profilertTil = V2PT.valueOf(profilertTil.name),
                    egenvurdering = V2PT.valueOf(pt.name)
                )
            )
        )
    }

    // Bekreftelse, en per Bekreftelsesloesning
    Bekreftelsesloesning.entries.forEachIndexed { i, bl ->
        add(
            Eksempel(
                bekreftelsemelding_v1, bl.name.lowercase(),
                bekreftelseMelding(
                    id = bekreftelseId,
                    periodeId = periodeId,
                    bekreftelsesloesning = bl,
                    gjelderFra = Instant.parse("2026-01-01T00:00:00.111111Z"),
                    gjelderTil = Instant.parse("2026-01-15T00:00:00.999999Z"),
                    harJobbetIDennePerioden = i % 2 == 0,
                    vilFortsetteSomArbeidssoeker = i % 2 == 1,
                    tidspunkt = tidspunkt
                ),
                V2Bekreftelse(
                    periodeId = periodeId,
                    bekreftelsesloesning = V2BL.valueOf(bl.name),
                    id = bekreftelseId,
                    svar = no.nav.paw.oppslagapi.model.v2.Svar(
                        sendtInnAv = V2Metadata(
                            tidspunkt = millis,
                            utfoertAv = no.nav.paw.oppslagapi.model.v2.Bruker(V2BT.SLUTTBRUKER, "test", null),
                            kilde = "test",
                            aarsak = "test",
                            tidspunktFraKilde = null
                        ),
                        gjelderFra = Instant.parse("2026-01-01T00:00:00.111Z"),
                        gjelderTil = Instant.parse("2026-01-15T00:00:00.999Z"),
                        harJobbetIDennePerioden = i % 2 == 0,
                        vilFortsetteSomArbeidssoeker = i % 2 == 1
                    )
                )
            )
        )
    }

    // PaaVegneAv start/stopp, alle Bekreftelsesloesning
    PaaVegneAvBekreftelsesloesning.entries.forEach { bl ->
        add(
            Eksempel(
                pa_vegne_av_start_v1, bl.name.lowercase(),
                startPaaVegneAv(periodeId, bl, Duration.ofDays(14), Duration.ofDays(7)),
                V2PaaVegneAvStart(periodeId, V2BL.valueOf(bl.name), Duration.ofDays(7).toMillis(), Duration.ofDays(14).toMillis())
            )
        )
    }
    listOf(true, false).forEach { fristBrutt ->
        add(
            Eksempel(
                pa_vegne_av_stopp_v1, "frist_brutt-$fristBrutt",
                stoppPaaVegneAv(periodeId, PaaVegneAvBekreftelsesloesning.DAGPENGER, fristBrutt),
                V2PaaVegneAvStopp(periodeId, V2BL.DAGPENGER, fristBrutt)
            )
        )
    }
}
