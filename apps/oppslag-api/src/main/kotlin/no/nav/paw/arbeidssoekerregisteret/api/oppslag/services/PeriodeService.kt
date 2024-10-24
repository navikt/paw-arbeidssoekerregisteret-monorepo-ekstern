package no.nav.paw.arbeidssoekerregisteret.api.oppslag.services

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ArbeidssoekerperiodeResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Paging
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.toArbeidssoekerperiodeResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.toPeriode
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.PeriodeRepository
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import java.util.*

class PeriodeService(private val periodeRepository: PeriodeRepository) {

    fun hentPeriodeForId(periodeId: UUID): Periode? = periodeRepository.hentPeriodeForId(periodeId)?.toPeriode()

    fun finnPerioderForIdentiteter(
        identitetsnummerList: List<Identitetsnummer>,
        paging: Paging = Paging()
    ): List<ArbeidssoekerperiodeResponse> =
        periodeRepository.finnPerioderForIdentiteter(identitetsnummerList, paging)
            .map { it.toArbeidssoekerperiodeResponse() }

    fun lagreAllePerioder(perioder: Sequence<Periode>) = periodeRepository.lagreAllePerioder(perioder)
}
