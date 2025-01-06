package no.nav.paw.arbeidssoekerregisteret.eksternt.api.services

import no.nav.paw.arbeidssoekerregisteret.eksternt.api.repositories.PeriodeRepository
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils.TimeUtils.getMaxDateForDatabaseStorage
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils.buildLogger
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils.getDelayUntilMidnight
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils.getDeletionInterval
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils.toInstant
import java.util.*

class ScheduleDeletionService(periodeRepository: PeriodeRepository) {
    private val logger = buildLogger

    private val timer = Timer()
    private val task =
        object : TimerTask() {
            override fun run() {
                try {
                    logger.info("Starter sletting av gammel data")
                    val rowsCount = periodeRepository
                        .slettDataEldreEnnDatoFraDatabase(getMaxDateForDatabaseStorage().toInstant())
                    logger.info("Slettet $rowsCount rader fra databasen")
                } catch (e: Exception) {
                    logger.error("Feil ved sletting av gammel data", e)
                }
            }
        }

    fun scheduleDatabaseDeletionTask() = timer.scheduleAtFixedRate(task, getDelayUntilMidnight(), getDeletionInterval())
}
