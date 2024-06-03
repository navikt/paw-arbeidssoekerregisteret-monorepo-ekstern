package no.nav.paw.arbeidssoekerregisteret.model

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssoekerregisteret.config.buildObjectMapper
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import java.time.Instant
import java.util.*

class PeriodeTest : FreeSpec({
    with(PeriodeTestContext()) {
        "Test suite for serialisering og deserialisering av Periode" - {
            "Skal serialisere aktiv periode" {
                val periodeStartetMetadata =
                    Metadata(
                        periodeStartetInstant,
                        Bruker(BrukerType.SLUTTBRUKER, identitetsnummer),
                        "paw",
                        "Start periode"
                    )
                val periode = Periode(periodeUuid, identitetsnummer, periodeStartetMetadata, null)
                val periodeInfo = periode.buildPeriodeInfo(arbeidssoekerId)

                val json = objectMapper.writeValueAsString(periodeInfo)
                json shouldBe aktivPeriodeJsonString
            }
            "Skal serialisere avsluttet periode" {
                val periodeStartetMetadata =
                    Metadata(
                        periodeStartetInstant,
                        Bruker(BrukerType.SLUTTBRUKER, identitetsnummer),
                        "paw",
                        "Start periode"
                    )
                val periodeAvsluttetMetadata =
                    Metadata(
                        periodeAvsluttetInstant,
                        Bruker(BrukerType.SLUTTBRUKER, identitetsnummer),
                        "paw",
                        "Start periode"
                    )
                val periode = Periode(periodeUuid, identitetsnummer, periodeStartetMetadata, periodeAvsluttetMetadata)
                val periodeInfo = periode.buildPeriodeInfo(arbeidssoekerId)

                val json = objectMapper.writeValueAsString(periodeInfo)
                json shouldBe avsluttetPeriodeJsonString
            }
        }
    }
})

private class PeriodeTestContext {
    val objectMapper = buildObjectMapper
    val periodeId = "65bf3691-fdd6-4bbc-b31e-2fd97501de45"
    val periodeUuid = UUID.fromString(periodeId)
    val identitetsnummer = "01017012345"
    val arbeidssoekerId = 98765L
    val periodeStartet = "2020-01-02T03:04:05.678Z"
    val periodeStartetInstant = Instant.parse(periodeStartet)
    val periodeAvsluttet = "2021-02-03T04:05:06.789Z"
    val periodeAvsluttetInstant = Instant.parse(periodeAvsluttet)
    val aktivPeriodeJsonString =
        """
           {"id":"${periodeUuid}","identitetsnummer":"${identitetsnummer}","arbeidssoekerId":${arbeidssoekerId},"startet":"${periodeStartet}"}
        """.trimIndent()
    val avsluttetPeriodeJsonString =
        """
           {"id":"${periodeUuid}","identitetsnummer":"${identitetsnummer}","arbeidssoekerId":${arbeidssoekerId},"startet":"${periodeStartet}","avsluttet":"${periodeAvsluttet}"}
        """.trimIndent()
}