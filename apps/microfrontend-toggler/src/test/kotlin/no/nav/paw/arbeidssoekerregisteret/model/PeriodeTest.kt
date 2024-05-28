package no.nav.paw.arbeidssoekerregisteret.model

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import java.time.Instant
import java.util.*

class PeriodeTest : FreeSpec({
    "Test mapping av aktiv periode" {
        val metadata = Metadata(Instant.now(), Bruker(BrukerType.SLUTTBRUKER, "123"), "PAW", "Start periode")
        val periode = Periode(UUID.randomUUID(), "123", metadata, null)
        val periodeInfo = PeriodeInfo(
            id = periode.id,
            identitetsnummer = periode.identitetsnummer,
            arbeidssoekerId = 123,
            startet = periode.startet.tidspunkt,
            avsluttet = periode.avsluttet?.tidspunkt
        )

        with(periodeInfo) {
            id shouldBe periode.id
            identitetsnummer shouldBe periode.identitetsnummer
            arbeidssoekerId shouldBe 123
            startet shouldBe periode.startet.tidspunkt
            avsluttet shouldBe null
        }
    }
})