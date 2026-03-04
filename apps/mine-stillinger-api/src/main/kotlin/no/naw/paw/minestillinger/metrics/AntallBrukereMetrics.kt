package no.naw.paw.minestillinger.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.opentelemetry.instrumentation.annotations.WithSpan
import kotlinx.coroutines.delay
import no.naw.paw.minestillinger.brukerprofil.flagg.HarBruktTjenestenFlaggtype
import no.naw.paw.minestillinger.brukerprofil.flagg.InkluderDirekteMeldteStillingerFlagtype
import no.naw.paw.minestillinger.brukerprofil.flagg.OptOutFlaggtype
import no.naw.paw.minestillinger.brukerprofil.flagg.StandardInnsatsFlaggtype
import no.naw.paw.minestillinger.brukerprofil.flagg.TjenestenErAktivFlaggtype
import no.naw.paw.minestillinger.db.BrukerFlaggTable
import no.naw.paw.minestillinger.db.BrukerTable
import no.naw.paw.minestillinger.db.ProfileringTable
import no.naw.paw.minestillinger.domain.ProfileringResultat
import org.jetbrains.exposed.v1.core.Case
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.booleanLiteral
import org.jetbrains.exposed.v1.core.countDistinct
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
                val tjenestenErAktiv = BrukerFlaggTable.alias("tjenesten_er_aktiv")
                val optOut = BrukerFlaggTable.alias("opt_out")
                val harBruktTjenesten = BrukerFlaggTable.alias("har_brukt_tjenesten")
                val standardInnsats = BrukerFlaggTable.alias("innsatsbehov")
                val inkluderDirekteMeldteStillinger = BrukerFlaggTable.alias("inkluder_direkte_meldte_stillinger")
                val aktivPeriode = Case()
                    .When(BrukerTable.arbeidssoekerperiodeAvsluttet.isNull(), booleanLiteral(true))
                    .Else(booleanLiteral(false))
                    .alias("aktiv_periode")
                BrukerTable
                    .join(
                        otherTable = tjenestenErAktiv,
                        joinType = JoinType.LEFT,
                        onColumn = BrukerTable.id,
                        otherColumn = tjenestenErAktiv[BrukerFlaggTable.brukerId],
                        additionalConstraint = { tjenestenErAktiv[BrukerFlaggTable.navn] eq TjenestenErAktivFlaggtype.type }
                    ).join(
                        otherTable = inkluderDirekteMeldteStillinger,
                        joinType = JoinType.LEFT,
                        onColumn = BrukerTable.id,
                        otherColumn = inkluderDirekteMeldteStillinger[BrukerFlaggTable.brukerId],
                        additionalConstraint = { inkluderDirekteMeldteStillinger[BrukerFlaggTable.navn] eq InkluderDirekteMeldteStillingerFlagtype.type }
                    )
                    .join(
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
                        otherTable = standardInnsats,
                        joinType = JoinType.LEFT,
                        onColumn = BrukerTable.id,
                        otherColumn = standardInnsats[BrukerFlaggTable.brukerId],
                        additionalConstraint = { standardInnsats[BrukerFlaggTable.navn] eq StandardInnsatsFlaggtype.type }
                    ).join(
                        otherTable = ProfileringTable,
                        joinType = JoinType.LEFT,
                        onColumn = BrukerTable.arbeidssoekerperiodeId,
                        otherColumn = ProfileringTable.periodeId,
                    ).select(
                        aktivPeriode,
                        tjenestenErAktiv[BrukerFlaggTable.verdi],
                        ProfileringTable.profileringResultat,
                        optOut[BrukerFlaggTable.verdi],
                        harBruktTjenesten[BrukerFlaggTable.verdi],
                        standardInnsats[BrukerFlaggTable.verdi],
                        BrukerTable.id.countDistinct(),
                    ).groupBy(
                        aktivPeriode,
                        tjenestenErAktiv[BrukerFlaggTable.verdi],
                        ProfileringTable.profileringResultat,
                        optOut[BrukerFlaggTable.verdi],
                        harBruktTjenesten[BrukerFlaggTable.verdi],
                        standardInnsats[BrukerFlaggTable.verdi],
                    ).map { row ->
                        MetricDataKey(
                            arbeidssoekerPeriodenErAktiv = row[aktivPeriode],
                            tjenestenErAktiv = row.getOrNull(tjenestenErAktiv[BrukerFlaggTable.verdi]) == true,
                            optOut = row.getOrNull(optOut[BrukerFlaggTable.verdi]) == true,
                            harBruktTjenesten = row.getOrNull(harBruktTjenesten[BrukerFlaggTable.verdi]) == true,
                            standardInnsats = row.getOrNull(standardInnsats[BrukerFlaggTable.verdi]) == true,
                            profileringsResultat =
                                row.getOrNull(ProfileringTable.profileringResultat)
                                    ?.let { ProfileringResultat.valueOf(it) }
                                    ?: ProfileringResultat.UDEFINERT,
                            inkluderDirekteMeldteStillinger = row.getOrNull(inkluderDirekteMeldteStillinger[BrukerFlaggTable.verdi]) == true,
                        ).let { key ->
                            MetricData(
                                key = key,
                                antall = row[BrukerTable.id.countDistinct()],
                            )
                        }
                    }.groupBy { it.key }
                    .map { (_, value) ->
                        if (value.size > 1) {
                            val antall = value.fold(0L, { acc, metricData -> acc + metricData.antall })
                            value.first().copy(antall = antall)
                        } else {
                            value.first()
                        }
                    }.associateBy { it.key }
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
                        meterRegistry.gauge(
                            "paw_mine_stillinger_antall_brukere",
                            listOf(
                                Tag.of("arbeidssoekerperioden_er_aktiv", key.arbeidssoekerPeriodenErAktiv.toString()),
                                Tag.of("tjenesten_er_aktiv", key.tjenestenErAktiv.toString()),
                                Tag.of("profilerings_resultat", key.profileringsResultat.name),
                                Tag.of("opt_out", key.optOut.toString()),
                                Tag.of("har_brukt_tjenesten", key.harBruktTjenesten.toString()),
                                Tag.of("standard_innsats", key.standardInnsats.toString()),
                                Tag.of("inkluder_direkte_meldte_stillinger", key.inkluderDirekteMeldteStillinger.toString()),
                            ),
                            atomicLong,
                            { it.get().toDouble() },
                        )
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
    val standardInnsats: Boolean,
    val inkluderDirekteMeldteStillinger: Boolean,
)

data class MetricData(
    val key: MetricDataKey,
    val antall: Long,
)

