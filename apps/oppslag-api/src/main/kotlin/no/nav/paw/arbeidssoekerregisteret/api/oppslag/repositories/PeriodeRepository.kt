package no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories

import io.micrometer.core.instrument.Tags
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.PeriodeFunctions
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.PeriodeTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Paging
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.PeriodeRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.TraceParent
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.buildLogger
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.initSpan
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.model.Identitetsnummer
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicLong

private const val forsinkelseGauge = "paw_oppslagsapi_consumer_forsinkelse_ms"
private const val sistLesteGauge = "paw-oppslagsapi_consumer_sist_leste_timestamp"
private const val typeLabel = "type"

class PeriodeRepository(
    private val prometheusMeterRegistry: PrometheusMeterRegistry
) {
    private val logger = buildLogger
    private val forsinkelse = AtomicLong(0L)
    private val sistLesteTimestamp = AtomicLong(0L)
    init {
        prometheusMeterRegistry.gauge(
            forsinkelseGauge,
            Tags.of(typeLabel, "periode"),
            forsinkelse
        ) { obj -> obj.get().toDouble() }
        prometheusMeterRegistry.gauge(
            sistLesteGauge,
            Tags.of(typeLabel, "periode"),
            sistLesteTimestamp
        ) { obj -> obj.get().toDouble() }
    }

    fun hentPeriodeForId(periodeId: UUID): PeriodeRow? =
        transaction {
            PeriodeFunctions.getForPeriodeId(periodeId)
        }

    fun finnPerioderForIdentiteter(
        identitetsnummerList: Collection<Identitetsnummer>,
        paging: Paging = Paging()
    ): List<PeriodeRow> =
        transaction {
            val rows = PeriodeFunctions.findForIdentitetsnummerList(identitetsnummerList, paging)
            if (paging.ordering == SortOrder.ASC) {
                rows.sortedBy { it.startet.tidspunkt }.take(paging.size)
            } else {
                rows.sortedByDescending { it.startet.tidspunkt }.take(paging.size)
            }
        }

    fun tellAntallAktivePerioder(): Long =
        transaction {
            PeriodeTable.selectAll().where { PeriodeTable.avsluttetId eq null }.count()
        }

    fun lagrePeriode(periode: Periode) {
        transaction {
            val eksisterendePeriode = PeriodeFunctions.getForPeriodeId(periode.id)
            if (eksisterendePeriode != null) {
                logger.info("Endrer eksisterende periode")
                PeriodeFunctions.update(periode, eksisterendePeriode)
            } else {
                logger.info("Lagrer ny periode")
                PeriodeFunctions.insert(periode)
            }
        }
    }

    fun lagrePerioder(perioder: Iterable<Pair<TraceParent?, Periode>>) {
        val initiell: Pair<Instant, Instant>? = null
        transaction {
            perioder.map { (traceparent, periode) ->
                initSpan(traceparent, "paw.kafka.consumer.periode", "periode process")
                    .use {
                        logger.info("Lagrer periode")
                        val eksisterendePeriode = PeriodeFunctions.getForPeriodeId(periode.id)
                        if (eksisterendePeriode != null) {
                            logger.info("Endrer eksisterende periode")
                            PeriodeFunctions.update(periode, eksisterendePeriode)
                        } else {
                            logger.info("Lagrer ny periode")
                            PeriodeFunctions.insert(periode)
                        }
                    }
                periode.avsluttet?.tidspunkt ?: periode.startet.tidspunkt
            }.fold(initiell) { acc, instant ->
                if (acc == null) {
                    instant to instant
                } else {
                    minOf(acc.first, instant) to maxOf(acc.second, instant)
                }
            }
        }?.also { (min, max) ->
            forsinkelse.set(System.currentTimeMillis() - min.toEpochMilli())
            sistLesteTimestamp.set(min.toEpochMilli())
        }
    }
}
