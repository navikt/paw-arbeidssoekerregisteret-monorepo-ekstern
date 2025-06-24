package no.nav.paw.oppslagapi.data.query

import io.kotest.core.spec.style.FreeSpec
import io.kotest.inspectors.shouldForAll
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.BekreftelseMedMetadata
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.BekreftelseMedMetadata.Status
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Bekreftelsesloesning.*
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.bekreftelse.melding.v1.vo.Bekreftelsesloesning.DAGPENGER as AVRO_BEKREFTELSE_DAGPENGER
import no.nav.paw.bekreftelse.paavegneav.v1.PaaVegneAv
import no.nav.paw.bekreftelse.paavegneav.v1.vo.Bekreftelsesloesning
import no.nav.paw.bekreftelse.paavegneav.v1.vo.Bekreftelsesloesning.DAGPENGER as AVRO_PAA_VEGNE_AV_DAGPENGER
import no.nav.paw.bekreftelse.paavegneav.v1.vo.Start
import no.nav.paw.bekreftelse.paavegneav.v1.vo.Stopp
import no.nav.paw.model.Identitetsnummer
import no.nav.paw.oppslagapi.data.Row
import no.nav.paw.oppslagapi.data.bekreftelsemelding_v1
import no.nav.paw.oppslagapi.data.consumer.converters.toOpenApi
import no.nav.paw.oppslagapi.data.pa_vegne_av_start_v1
import no.nav.paw.oppslagapi.data.pa_vegne_av_stopp_v1
import no.nav.paw.oppslagapi.data.periode_avsluttet_v1
import no.nav.paw.oppslagapi.data.periode_startet_v1
import no.nav.paw.oppslagsapi.periode
import no.nav.paw.oppslagsapi.person1
import no.nav.paw.test.data.bekreftelse.bekreftelseMelding
import no.nav.paw.test.data.bekreftelse.startPaaVegneAv
import no.nav.paw.test.data.bekreftelse.stoppPaaVegneAv
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Bekreftelse as OpenApiBekreftelse

class TidslinjegeneratorTest: FreeSpec({
    "Tidslinjegenerator" - {
        "Verifiser standard flyt åpen periode" {
            val startTid = Instant.now().truncatedTo(ChronoUnit.MILLIS)
            val periode = periode(identitetsnummer = person1.first(), startet = startTid)
            val rader = listOf(
                row(periode),
                row(bekreftelseMelding(periodeId = periode.id, tidspunkt = startTid + 1.dager)),
                row(startPaaVegneAv(periodeId = periode.id, bekreftelsesloesning = AVRO_PAA_VEGNE_AV_DAGPENGER), timestamp = startTid + 2.dager),
                row(bekreftelseMelding(periodeId = periode.id, tidspunkt = startTid + 3.dager, bekreftelsesloesning = AVRO_BEKREFTELSE_DAGPENGER)),
                row(stoppPaaVegneAv(periodeId = periode.id, bekreftelsesloesning = AVRO_PAA_VEGNE_AV_DAGPENGER), timestamp = startTid + 4.dager),
                row(bekreftelseMelding(periodeId = periode.id, tidspunkt = startTid + 5.dager))
            )
            val tidslinje = genererTidslinje(periodeId = periode.id, rader = rader)
            tidslinje.shouldNotBeNull()
            tidslinje.identitetsnummer shouldBe periode.identitetsnummer
            tidslinje.periodeStart shouldBe startTid
            tidslinje.periodeStopp shouldBe null
            tidslinje.bekreftelser should { bekreftelser ->
                bekreftelser.size shouldBe 3
                bekreftelser.shouldForAll { bekreftelse -> bekreftelse.status shouldBe Status.GYLDIG}
            }
        }
        "Verifiser standard flyt avsluttet periode" {
            val startTid = Instant.now().truncatedTo(ChronoUnit.MILLIS)
            val periode = periode(identitetsnummer = person1.first(), startet = startTid)
            val rader = listOf(
                row(periode),
                row(bekreftelseMelding(periodeId = periode.id, tidspunkt = startTid + 1.dager)),
                row(startPaaVegneAv(periodeId = periode.id, bekreftelsesloesning = AVRO_PAA_VEGNE_AV_DAGPENGER), timestamp = startTid + 2.dager),
                row(bekreftelseMelding(periodeId = periode.id, tidspunkt = startTid + 3.dager, bekreftelsesloesning = AVRO_BEKREFTELSE_DAGPENGER)),
                row(stoppPaaVegneAv(periodeId = periode.id, bekreftelsesloesning = AVRO_PAA_VEGNE_AV_DAGPENGER), timestamp = startTid + 4.dager),
                row(bekreftelseMelding(periodeId = periode.id, tidspunkt = startTid + 5.dager)),
                row(periode(
                    identitetsnummer = Identitetsnummer(periode.identitetsnummer),
                    periodeId = periode.id,
                    startet = periode.startet.tidspunkt,
                    avsluttet = startTid + 6.dager
                ))
            )
            val tidslinje = genererTidslinje(periodeId = periode.id, rader = rader)
            tidslinje.shouldNotBeNull()
            tidslinje.identitetsnummer shouldBe periode.identitetsnummer
            tidslinje.periodeStart shouldBe startTid
            tidslinje.periodeStopp shouldBe startTid + 6.dager
            tidslinje.bekreftelser should { bekreftelser ->
                bekreftelser.size shouldBe 3
                bekreftelser.shouldForAll { bekreftelse -> bekreftelse.status shouldBe Status.GYLDIG}
            }
        }
        "Verifiser uventet avsender" {
            val startTid = Instant.now().truncatedTo(ChronoUnit.MILLIS)
            val periode = periode(identitetsnummer = person1.first(), startet = startTid)
            val rader = listOf(
                row(periode),
                row(bekreftelseMelding(periodeId = periode.id, tidspunkt = startTid + 1.dager)),
                row(bekreftelseMelding(periodeId = periode.id, tidspunkt = startTid + 3.dager, bekreftelsesloesning = AVRO_BEKREFTELSE_DAGPENGER)),
                row(stoppPaaVegneAv(periodeId = periode.id, bekreftelsesloesning = AVRO_PAA_VEGNE_AV_DAGPENGER), timestamp = startTid + 4.dager),
                row(bekreftelseMelding(periodeId = periode.id, tidspunkt = startTid + 5.dager))
            )
            val tidslinje = genererTidslinje(periodeId = periode.id, rader = rader)
            tidslinje.shouldNotBeNull()
            tidslinje.identitetsnummer shouldBe periode.identitetsnummer
            tidslinje.periodeStart shouldBe startTid
            tidslinje.periodeStopp shouldBe null
            tidslinje.bekreftelser should { bekreftelser ->
                bekreftelser.size shouldBe 3
                bekreftelser.filter { it.status == Status.GYLDIG } should { gyldige ->
                    gyldige.size shouldBe 2
                    gyldige.shouldForAll { bekreftelse -> bekreftelse.bekreftelse?.bekreftelsesloesning shouldBe ARBEIDSSOEKERREGISTERET }
                }
                bekreftelser.filter { it.status == Status.UVENTET_KILDE } should { uventetKilde ->
                    uventetKilde.size shouldBe 1
                    uventetKilde.first().bekreftelse?.bekreftelsesloesning shouldBe DAGPENGER
                }
            }
        }
    }
})


fun row(bekreftelse: Bekreftelse): Row<OpenApiBekreftelse> =
    Row(
        type = bekreftelsemelding_v1,
        identitetsnummer = null,
        periodeId = bekreftelse.periodeId,
        timestamp = bekreftelse.svar.sendtInnAv.tidspunkt,
        data = bekreftelse.toOpenApi()
    )

fun row(paaVegneAv: PaaVegneAv, timestamp: Instant = Instant.now()): Row<Any> {
    val handling = paaVegneAv.handling
    return when (handling) {
        is Start -> {
            Row(
                type = pa_vegne_av_start_v1,
                identitetsnummer = null,
                periodeId = paaVegneAv.periodeId,
                timestamp = timestamp,
                data = handling.toOpenApi(paaVegneAv)
            )
        }
        is Stopp -> {
            Row(
                type = pa_vegne_av_stopp_v1,
                identitetsnummer = null,
                periodeId = paaVegneAv.periodeId,
                timestamp = timestamp,
                data = handling.toOpenApi(paaVegneAv)
            )
        }
        else -> throw IllegalArgumentException("Handling $handling is not implemented for Tidslinje")
    }
}

fun row(periode: Periode): Row<Metadata> {
    val avsluttet = periode.avsluttet
    return if (avsluttet == null) {
        Row(
            type= periode_startet_v1,
            identitetsnummer = periode.identitetsnummer,
            periodeId = periode.id,
            timestamp = periode.startet.tidspunkt,
            data = periode.startet.toOpenApi()
        )
    } else {
        Row(
            type= periode_avsluttet_v1,
            identitetsnummer = periode.identitetsnummer,
            periodeId = periode.id,
            timestamp = avsluttet.tidspunkt,
            data = avsluttet.toOpenApi()
        )
    }
}

val Int.dager: Duration get() = Duration.ofDays(this.toLong())