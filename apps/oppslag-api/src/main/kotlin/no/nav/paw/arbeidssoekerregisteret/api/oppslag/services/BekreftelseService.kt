package no.nav.paw.arbeidssoekerregisteret.api.oppslag.services

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.BekreftelseResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.toBekreftelseResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.BekreftelseRepository
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import java.util.*

class BekreftelseService(private val bekreftelseRepository: BekreftelseRepository) {

    fun finnBekreftelserForPeriodeId(periodeId: UUID): List<BekreftelseResponse> =
        bekreftelseRepository.finnBekreftelserForPeriodeId(periodeId).map { it.toBekreftelseResponse() }

    fun lagreBekreftelse(batch: Sequence<Bekreftelse>) {
        batch.forEach(bekreftelseRepository::opprettBekreftelse)
    }
}