package no.naw.paw.minestillinger.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.opentelemetry.instrumentation.annotations.WithSpan
import kotlinx.coroutines.delay
import no.naw.paw.minestillinger.appLogger
import no.naw.paw.minestillinger.brukerprofil.flagg.HarBruktTjenestenFlaggtype
import no.naw.paw.minestillinger.brukerprofil.flagg.OptOutFlaggtype
import no.naw.paw.minestillinger.brukerprofil.flagg.TjenestenErAktivFlaggtype
import no.naw.paw.minestillinger.db.BrukerFlaggTable
import no.naw.paw.minestillinger.db.BrukerTable
import no.naw.paw.minestillinger.db.ProfileringTable
import no.naw.paw.minestillinger.domain.ProfileringResultat
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong

class AntallBrukereMetrics(
    val meterRegistry: MeterRegistry,
) {
    private val metricsMap = HashMap<MetricDataKey, AtomicLong>()
    fun snapshot() = metricsMap.toList()

    suspend fun startPeriodiskOppdateringAvMetrics() {
        while (true) {
            oppdaterAntallBrukere()
            delay(timeMillis = Duration.ofMinutes(10).toMillis())
        }
    }

    @kotlin.jvm.Synchronized
    @WithSpan("metrics_oppdater_antall_brukere")
    fun oppdaterAntallBrukere() {
        val data =
            transaction {
                val debugInfoAntallProfiler = BrukerTable.select(BrukerFlaggTable.id).count()
                val debuigInfoAntallAktivePerioder = BrukerTable.select(BrukerFlaggTable.id)
                    .where { BrukerTable.arbeidssoekerperiodeAvsluttet.isNull() }
                    .count()
                appLogger.info("debugInfoAntallProfiler: $debugInfoAntallProfiler")
                appLogger.info("debugInfoAntallAktivePerioder: $debuigInfoAntallAktivePerioder")
                val tjenestenErAktiv = BrukerFlaggTable.alias("tjenesten_er_aktiv")
                val optOut = BrukerFlaggTable.alias("opt_out")
                val harBruktTjenesten = BrukerFlaggTable.alias("har_brukt_tjenesten")
                BrukerTable
                    .join(
                        otherTable = tjenestenErAktiv,
                        joinType = JoinType.LEFT,
                        onColumn = BrukerTable.id,
                        otherColumn = tjenestenErAktiv[BrukerFlaggTable.brukerId],
                        additionalConstraint = { tjenestenErAktiv[BrukerFlaggTable.navn] eq TjenestenErAktivFlaggtype.type }
                    ).join(
                        otherTable = optOut,
                        joinType = JoinType.LEFT,
                        onColumn = BrukerTable.id,
                        otherColumn = optOut[BrukerFlaggTable.brukerId],
                        additionalConstraint = { optOut[BrukerFlaggTable.navn] eq OptOutFlaggtype.type }
                    ).join(
                        otherTable = harBruktTjenesten,
                        joinType = JoinType.LEFT,
                        onColumn = BrukerTable.id,
                        otherColumn = harBruktTjenesten[BrukerFlaggTable.brukerId],
                        additionalConstraint = { harBruktTjenesten[BrukerFlaggTable.navn] eq HarBruktTjenestenFlaggtype.type }
                    ).join(
                        otherTable = ProfileringTable,
                        joinType = JoinType.LEFT,
                        onColumn = BrukerTable.arbeidssoekerperiodeId,
                        otherColumn = ProfileringTable.periodeId,
                    ).select(
                        BrukerTable.arbeidssoekerperiodeAvsluttet,
                        tjenestenErAktiv[BrukerFlaggTable.verdi],
                        ProfileringTable.profileringResultat,
                        optOut[BrukerFlaggTable.verdi],
                        harBruktTjenesten[BrukerFlaggTable.verdi],
                        BrukerTable.id.count(),
                    ).groupBy(
                        BrukerTable.arbeidssoekerperiodeAvsluttet,
                        tjenestenErAktiv[BrukerFlaggTable.verdi],
                        ProfileringTable.profileringResultat,
                        optOut[BrukerFlaggTable.verdi],
                        harBruktTjenesten[BrukerFlaggTable.verdi],
                    ).map { row ->
                        MetricDataKey(
                            arbeidssoekerPeriodenErAktiv = row[BrukerTable.arbeidssoekerperiodeAvsluttet] == null,
                            tjenestenErAktiv = row.getOrNull(tjenestenErAktiv[BrukerFlaggTable.verdi]) == true,
                            optOut = row.getOrNull(optOut[BrukerFlaggTable.verdi]) == true,
                            harBruktTjenesten = row.getOrNull(harBruktTjenesten[BrukerFlaggTable.verdi]) == true,
                            profileringsResultat =
                                row.getOrNull(ProfileringTable.profileringResultat)
                                    ?.let { ProfileringResultat.valueOf(it) }
                                    ?: ProfileringResultat.UDEFINERT,
                        ).let { key ->
                            MetricData(
                                key = key,
                                antall = row[BrukerTable.id.count()],
                            )
                        }
                    }.associateBy { it.key }
            }
        data.forEach { kv ->
            appLogger.info("Antall brukere - nøkkel: ${kv.key}, antall: ${kv.value.antall}")
        }
        (metricsMap.keys + data.keys)
            .distinct()
            .map { key -> key to (data[key]?.antall ?: 0L) }
            .forEach { (key, value) ->
                metricsMap.compute(key) { _, existing ->
                    if (existing != null) {
                        existing.set(value)
                        existing
                    } else {
                        val atomicLong = AtomicLong(value)
                        appLogger.info("Registrerer ny metrikk for nøkkel: $key")
                        meterRegistry.gauge(
                            "paw_mine_stillinger_antall_brukere",
                            listOf(
                                Tag.of("arbeidssoekerperioden_er_aktiv", key.arbeidssoekerPeriodenErAktiv.toString()),
                                Tag.of("tjenesten_er_aktiv", key.tjenestenErAktiv.toString()),
                                Tag.of("profilerings_resultat", key.profileringsResultat.name),
                                Tag.of("opt_out", key.optOut.toString()),
                                Tag.of("har_brukt_tjenesten", key.harBruktTjenesten.toString()),
                            ),
                            atomicLong,
                            { it.get().toDouble() },
                        )
                        appLogger.info("Regsterering fullført")
                        atomicLong
                    }
                }
            }
    }
}

data class MetricDataKey(
    val arbeidssoekerPeriodenErAktiv: Boolean,
    val tjenestenErAktiv: Boolean,
    val optOut: Boolean,
    val harBruktTjenesten: Boolean,
    val profileringsResultat: ProfileringResultat,
)

data class MetricData(
    val key: MetricDataKey,
    val antall: Long,
)

