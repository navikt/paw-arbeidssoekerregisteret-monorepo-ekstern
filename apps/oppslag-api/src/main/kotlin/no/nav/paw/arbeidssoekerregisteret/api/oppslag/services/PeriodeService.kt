package no.nav.paw.arbeidssoekerregisteret.api.oppslag.services

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.PeriodeRepository
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import java.util.*

class PeriodeService(private val periodeRepository: PeriodeRepository) {

    fun finnPerioderForIdentiteter(identitetsnummerList: List<Identitetsnummer>) =
        periodeRepository.finnPerioderForIdentiteter(identitetsnummerList)

    fun hentPeriodeForId(periodeId: UUID): Periode? = periodeRepository.hentPeriodeForId(periodeId)

    fun lagreAllePerioder(periodeList: Sequence<Periode>) = periodeRepository.lagreAllePerioder(periodeList)
}
