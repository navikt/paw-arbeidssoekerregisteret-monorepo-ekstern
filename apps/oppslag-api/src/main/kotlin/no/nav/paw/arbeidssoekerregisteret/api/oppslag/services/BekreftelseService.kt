package no.nav.paw.arbeidssoekerregisteret.api.oppslag.services

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.BekreftelseResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Paging
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.toBekreftelseResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.BekreftelseRepository
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.security.authentication.model.Identitetsnummer
import java.util.*

class BekreftelseService(private val bekreftelseRepository: BekreftelseRepository) {

    fun finnBekreftelserForPeriodeIdList(
        periodeIdList: Collection<UUID>,
        paging: Paging = Paging()
    ): List<BekreftelseResponse> =
        bekreftelseRepository.finnBekreftelserForPeriodeIdList(periodeIdList, paging)
            .map { it.toBekreftelseResponse() }

    fun finnBekreftelserForIdentitetsnummerList(
        identitetsnummerList: Collection<Identitetsnummer>,
        paging: Paging = Paging()
    ): List<BekreftelseResponse> =
        bekreftelseRepository.finnBekreftelserForIdentitetsnummerList(identitetsnummerList, paging)
            .map { it.toBekreftelseResponse() }

    fun lagreAlleBekreftelser(bekreftelser: Sequence<Bekreftelse>) {
        bekreftelseRepository.lagreAlleBekreftelser(bekreftelser)
    }
}