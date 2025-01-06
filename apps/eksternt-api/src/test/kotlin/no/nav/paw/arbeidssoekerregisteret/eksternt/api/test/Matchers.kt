package no.nav.paw.arbeidssoekerregisteret.eksternt.api.test

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.models.PeriodeRow
import java.time.Instant
import java.time.temporal.ChronoUnit

infix fun PeriodeRow.shouldBeEqualTo(other: PeriodeRow?): PeriodeRow {
    other shouldNotBe null
    periodeId shouldBe other?.periodeId
    identitetsnummer shouldBe other?.identitetsnummer
    startet shouldBeEqualTo other?.startet
    avsluttet?.shouldBeEqualTo(other?.avsluttet)
    return this
}

infix fun Instant.shouldBeEqualTo(other: Instant?): Instant {
    other shouldNotBe null
    this.truncatedTo(ChronoUnit.SECONDS) shouldBe other?.truncatedTo(ChronoUnit.SECONDS)
    return this
}
