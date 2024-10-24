package no.nav.paw.arbeidssoekerregisteret.eksternt.api.services

import no.nav.paw.arbeidssoekerregisteret.eksternt.api.models.ArbeidssoekerperiodeResponse
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.models.toArbeidssoekerperiodeResponse
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.repositories.ArbeidssoekerperiodeRepository
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import java.time.LocalDate

class ArbeidssoekerService(private val arbeidssoekerperiodeRepository: ArbeidssoekerperiodeRepository) {

    fun lagreAlleArbeidssoekerperioder(arbeidssoekerperioder: Sequence<Periode>) {
        arbeidssoekerperiodeRepository.lagreAlleArbeidssoekerperioder(arbeidssoekerperioder)
    }

    fun hentArbeidssoekerperioder(
        identitetsnummer: Identitetsnummer,
        fraStartetDato: LocalDate?
    ): List<ArbeidssoekerperiodeResponse> = arbeidssoekerperiodeRepository
        .hentArbeidssoekerperioder(identitetsnummer, fraStartetDato)
        .map { it.toArbeidssoekerperiodeResponse() }
}
