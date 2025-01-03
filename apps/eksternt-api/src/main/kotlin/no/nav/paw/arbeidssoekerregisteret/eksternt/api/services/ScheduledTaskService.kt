package no.nav.paw.arbeidssoekerregisteret.eksternt.api.services

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.repositories.PeriodeRepository
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils.TimeUtils
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils.antallAktivePerioder
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils.buildLogger

class ScheduledTaskService(
    private val meterRegistry: PrometheusMeterRegistry,
    private val periodeRepository: PeriodeRepository
) {
    private val logger = buildLogger

    fun perioderVedlikeholdTask() {
        try {
            val maksAlder = TimeUtils.maksAlderForData()
            logger.info("Starter sletting av gammel data")
            val rowsCount = periodeRepository
                .slettMedStartetEldreEnn(maksAlder)
            logger.info("Slettet $rowsCount rader fra databasen")
        } catch (e: Exception) {
            logger.error("Feil ved sletting av gammel data", e)
        }
    }

    fun perioderMetricsTask() {
        val antallAktivePerioder = periodeRepository.hentAntallAktivePerioder()
        meterRegistry.antallAktivePerioder(antallAktivePerioder)
    }
}
