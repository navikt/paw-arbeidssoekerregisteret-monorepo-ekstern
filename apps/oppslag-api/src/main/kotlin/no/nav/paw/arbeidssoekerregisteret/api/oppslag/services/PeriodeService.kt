package no.nav.paw.arbeidssoekerregisteret.api.oppslag.services

import io.ktor.http.HttpStatusCode
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.StatusException
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.PeriodeRepository
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import java.util.*

class PeriodeService(private val periodeRepository: PeriodeRepository) {

    fun finnPerioderForIdentiteter(identitetsnummerList: List<Identitetsnummer>) =
        periodeRepository.finnPerioderForIdentiteter(identitetsnummerList)

    fun periodeTilhoererIdentiteter(
        periodeId: UUID,
        identitetsnummerList: List<Identitetsnummer>
    ): Boolean {
        val periode = periodeRepository.finnPerioderForId(periodeId) ?: throw StatusException(
            HttpStatusCode.BadRequest,
            "Finner ikke periode"
        )
        return identitetsnummerList.map { it.verdi }.contains(periode.identitetsnummer)
    }

    fun lagreAllePerioder(periodeList: Sequence<Periode>) = periodeRepository.lagreAllePerioder(periodeList)
}
