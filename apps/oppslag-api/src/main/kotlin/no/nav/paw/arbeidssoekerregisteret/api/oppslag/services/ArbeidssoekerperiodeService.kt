package no.nav.paw.arbeidssoekerregisteret.api.oppslag.services

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.ArbeidssoekerperiodeRepository
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import java.util.*

class ArbeidssoekerperiodeService(private val arbeidssoekerperiodeRepository: ArbeidssoekerperiodeRepository) {

    fun hentArbeidssoekerperioder(identitetsnummer: Identitetsnummer) = arbeidssoekerperiodeRepository.hentArbeidssoekerperioder(identitetsnummer)

    fun periodeIdTilhoererIdentitetsnummer(
        periodeId: UUID,
        identitetsnummer: Identitetsnummer
    ): Boolean = periodeId in arbeidssoekerperiodeRepository.hentArbeidssoekerperioder(identitetsnummer).map { it.periodeId }

    fun lagreBatch(batch: Sequence<Periode>) = arbeidssoekerperiodeRepository.lagreArbeidssoekerperioder(batch)
}
