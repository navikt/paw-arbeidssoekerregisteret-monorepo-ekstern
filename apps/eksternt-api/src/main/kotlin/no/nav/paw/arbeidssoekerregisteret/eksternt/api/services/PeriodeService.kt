package no.nav.paw.arbeidssoekerregisteret.eksternt.api.services

import no.nav.paw.arbeidssoekerregisteret.eksternt.api.models.ArbeidssoekerperiodeResponse
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.models.asArbeidssoekerperiodeResponse
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.repositories.PeriodeRepository
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils.buildLogger
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import org.apache.kafka.clients.consumer.ConsumerRecords
import java.time.LocalDate

class PeriodeService(private val periodeRepository: PeriodeRepository) {

    private val logger = buildLogger

    fun hentPerioder(
        identitetsnummer: Identitetsnummer,
        fraStartetDato: LocalDate? = null
    ): List<ArbeidssoekerperiodeResponse> = periodeRepository
        .finnPerioder(identitetsnummer, fraStartetDato)
        .map { it.asArbeidssoekerperiodeResponse() }


    fun lagreAllePerioder(perioder: Iterable<Periode>) {
        logger.debug("Lagrer perioder")
        periodeRepository.lagreAllePerioder(perioder)
    }

    fun handleRecords(records: ConsumerRecords<Long, Periode>) {
        if (records.count() > 0) {
            logger.info("Mottok {} perioder fra Kafka", records.count())
            lagreAllePerioder(records.map { it.value() })
        }
    }
}
