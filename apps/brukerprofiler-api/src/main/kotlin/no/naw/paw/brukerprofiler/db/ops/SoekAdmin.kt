package no.naw.paw.brukerprofiler.db.ops

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.naw.paw.brukerprofiler.db.SoekTable
import no.naw.paw.brukerprofiler.domain.LagretStillingsoek
import no.naw.paw.brukerprofiler.domain.ReiseveiSoek
import no.naw.paw.brukerprofiler.domain.StedSoek
import no.naw.paw.brukerprofiler.domain.Stillingssoek
import no.naw.paw.brukerprofiler.domain.StillingssoekType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.reflect.KClass

private val soekObjectMapper: ObjectMapper = ObjectMapper()
    .registerKotlinModule()

fun lagreSoek(brukerId: Long, tidspunkt: Instant, soek: Stillingssoek) {
    SoekTable.insert {
        it[SoekTable.brukerId] = brukerId
        it[SoekTable.type] = soek.soekType.name
        it[SoekTable.soek] = soekObjectMapper.writeValueAsString(soek)
        it[SoekTable.opprettet] = tidspunkt.truncatedTo(ChronoUnit.MILLIS)
    }
}

fun hentSoek(brukerId: Long): List<LagretStillingsoek> {
    return SoekTable.selectAll()
        .where { SoekTable.brukerId eq brukerId }
        .map { row ->
            val soekeType = StillingssoekType.valueOf(row[SoekTable.type])
            val soek = soekObjectMapper.readValue(row[SoekTable.soek], soekeType.toClass().java)
            LagretStillingsoek(
                id = row[SoekTable.id],
                brukerId = row[SoekTable.brukerId],
                opprettet = row[SoekTable.opprettet],
                soek = soek
            )
        }
}

fun StillingssoekType.toClass(): KClass<out Stillingssoek> = when (this) {
    StillingssoekType.STED_SOEK_V1 -> StedSoek::class
    StillingssoekType.REISEVEI_SOEK_V1 -> ReiseveiSoek::class
}

fun slettAlleSoekForBruker(brukerId: Long): Int {
    return SoekTable.deleteWhere { SoekTable.brukerId eq brukerId }
}

fun slettSoek(brukerId: Long, soekId: Long): Int {
    return SoekTable.deleteWhere { (SoekTable.brukerId eq brukerId) and (SoekTable.id eq soekId) }
}