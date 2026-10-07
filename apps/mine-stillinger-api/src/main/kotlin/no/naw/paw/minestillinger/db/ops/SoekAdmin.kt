package no.naw.paw.minestillinger.db.ops

import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.MapperFeature
import tools.jackson.databind.cfg.EnumFeature
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.module.kotlin.KotlinFeature
import tools.jackson.module.kotlin.jacksonMapperBuilder
import no.naw.paw.minestillinger.db.SoekTable
import no.naw.paw.minestillinger.domain.BrukerId
import no.naw.paw.minestillinger.domain.LagretStillingsoek
import no.naw.paw.minestillinger.domain.ReiseveiSoek
import no.naw.paw.minestillinger.domain.StedSoek
import no.naw.paw.minestillinger.domain.Stillingssoek
import no.naw.paw.minestillinger.domain.StillingssoekType
import no.naw.paw.minestillinger.domain.SøkId
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.reflect.KClass


interface SøkAdminOps {
    fun lagreSoek(brukerId: BrukerId, tidspunkt: Instant, soek: Stillingssoek): Unit
    fun hentSoek(brukerId: BrukerId): List<LagretStillingsoek>
    fun slettAlleSoekForBruker(brukerId: BrukerId): Int
    fun slettSoek(brukerId: BrukerId, soekId: SøkId): Int
    fun settSistKjørt(søkId: SøkId, tidspunkt: Instant): Boolean
}

object ExposedSøkAdminOps : SøkAdminOps {
    override fun lagreSoek(
        brukerId: BrukerId,
        tidspunkt: Instant,
        soek: Stillingssoek
    ) = no.naw.paw.minestillinger.db.ops.lagreSoek(brukerId, tidspunkt, soek)

    override fun hentSoek(brukerId: BrukerId): List<LagretStillingsoek> =
        no.naw.paw.minestillinger.db.ops.hentSoek(brukerId)

    override fun slettAlleSoekForBruker(brukerId: BrukerId): Int =
        no.naw.paw.minestillinger.db.ops.slettAlleSoekForBruker(brukerId)

    override fun slettSoek(brukerId: BrukerId, soekId: SøkId): Int =
        no.naw.paw.minestillinger.db.ops.slettSoek(brukerId, soekId)

    override fun settSistKjørt(
        søkId: SøkId,
        tidspunkt: Instant
    ): Boolean = no.naw.paw.minestillinger.db.ops.settSistKjørt(søkId, tidspunkt)

}

private val soekObjectMapper: ObjectMapper = jacksonMapperBuilder {
    disable(KotlinFeature.SingletonSupport)
    disable(KotlinFeature.StrictNullChecks)
}
    .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
    .enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, DateTimeFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
    .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
    .disable(EnumFeature.READ_ENUMS_USING_TO_STRING, EnumFeature.WRITE_ENUMS_USING_TO_STRING)
    .build()

fun lagreSoek(brukerId: BrukerId, tidspunkt: Instant, soek: Stillingssoek) {
    SoekTable.insert {
        it[SoekTable.brukerId] = brukerId.verdi
        it[SoekTable.type] = soek.soekType.name
        it[SoekTable.soek] = soekObjectMapper.writeValueAsString(soek)
        it[SoekTable.opprettet] = tidspunkt.truncatedTo(ChronoUnit.MILLIS)
        it[SoekTable.sistKjoert] = null
    }
}

fun settSistKjørt(søkId: SøkId, tidspunkt: Instant): Boolean =
    SoekTable.update({ SoekTable.id eq søkId.verdi }) {
        it[sistKjoert] = tidspunkt.truncatedTo(ChronoUnit.MILLIS)
    } == 1

fun hentSoek(brukerId: BrukerId): List<LagretStillingsoek> {
    return SoekTable.selectAll()
        .where { SoekTable.brukerId eq brukerId.verdi }
        .map { row ->
            val soekeType = StillingssoekType.valueOf(row[SoekTable.type])
            val soek = soekObjectMapper.readValue(row[SoekTable.soek], soekeType.toClass().java)
            LagretStillingsoek(
                id = SøkId(row[SoekTable.id]),
                brukerId = row[SoekTable.brukerId],
                opprettet = row[SoekTable.opprettet],
                sistKjoet = row[SoekTable.sistKjoert],
                soek = soek
            )
        }
}

fun StillingssoekType.toClass(): KClass<out Stillingssoek> = when (this) {
    StillingssoekType.STED_SOEK_V1 -> StedSoek::class
    StillingssoekType.REISEVEI_SOEK_V1 -> ReiseveiSoek::class
}

fun slettAlleSoekForBruker(brukerId: BrukerId): Int {
    return SoekTable.deleteWhere { SoekTable.brukerId eq brukerId.verdi }
}

fun slettSoek(brukerId: BrukerId, soekId: SøkId): Int {
    return SoekTable.deleteWhere { (SoekTable.brukerId eq brukerId.verdi) and (SoekTable.id eq soekId.verdi) }
}
