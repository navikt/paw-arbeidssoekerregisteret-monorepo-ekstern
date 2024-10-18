package no.nav.paw.arbeidssoekerregisteret.api.oppslag.services

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.BekreftelseRepository
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.buildLogger
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import java.util.*

class BekreftelseService(private val bekreftelseRepository: BekreftelseRepository) {
    private val logger = buildLogger

    fun finnBekreftelserForPeriodeId(periodeId: UUID) = bekreftelseRepository.finnBekreftelserForPeriodeId(periodeId)

    fun hentBekreftelseForId(bekreftelseMeldingId: UUID) =
        bekreftelseRepository.hentBekreftelseForId(bekreftelseMeldingId)

    fun lagreBekreftelse(batch: Sequence<Bekreftelse>) {
        batch.filter { bekreftelse ->
            (hentBekreftelseForId(bekreftelse.id) == null)
                .also {
                    if (!it) logger.info("Bekreftelse med id ${bekreftelse.id} finnes allerede")
                }
        }.forEach(bekreftelseRepository::opprettBekreftelse)
    }
}