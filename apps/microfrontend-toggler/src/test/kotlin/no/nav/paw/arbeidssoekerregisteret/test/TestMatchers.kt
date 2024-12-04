package no.nav.paw.arbeidssoekerregisteret.test

import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssoekerregisteret.model.PeriodeInfo
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode

infix fun PeriodeInfo.shouldBeEqualTo(periode: Periode) {
    id shouldBe periode.id
    identitetsnummer shouldBe periode.identitetsnummer
    startet shouldBe periode.startet.tidspunkt
    avsluttet shouldBe periode.avsluttet?.tidspunkt
}