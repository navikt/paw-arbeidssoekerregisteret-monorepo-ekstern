package no.nav.paw.oppslagapi.data.query

import no.nav.paw.model.Identitetsnummer
import no.nav.paw.oppslagapi.data.DataTable
import no.nav.paw.oppslagapi.data.Row
import no.nav.paw.oppslagapi.data.objectMapper
import no.nav.paw.oppslagapi.data.typeTilKlasse
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.*
import kotlin.reflect.full.isSubclassOf

interface DatabaseQeurySupport {
    fun hentRaderForPeriode(
        periodeId: UUID
    ): List<Row<Any>>

    fun hentPerioder(
        identitetsnummer: Identitetsnummer
    ): List<UUID>
}

object ExposedDatabaseQuerySupport : DatabaseQeurySupport {
    override fun hentRaderForPeriode(
        periodeId: UUID
    ): List<Row<Any>> =
        transaction {
            DataTable.selectAll()
                .where { DataTable.periodeId eq periodeId }
                .map(::asObject)
        }

    override fun hentPerioder(
        identitetsnummer: Identitetsnummer
    ): List<UUID> =
        transaction {
            DataTable.select(DataTable.periodeId)
                .withDistinctOn(DataTable.periodeId)
                .where(DataTable.identitetsnummer eq identitetsnummer.verdi)
                .map { it[DataTable.periodeId] }
        }
}

val exposedDatabaseQuerySupport: DatabaseQeurySupport get() = ExposedDatabaseQuerySupport

fun asObject(row: ResultRow): Row<Any> {
    val type = row[DataTable.type]
    val clazz = typeTilKlasse[type]?.java
        ?: throw IllegalArgumentException("Ukjent type: $type")
    return Row(
        type = type,
        identitetsnummer = row[DataTable.identitetsnummer],
        periodeId = row[DataTable.periodeId],
        timestamp = row[DataTable.timestamp],
        data = objectMapper.readValue(row[DataTable.data], clazz)
    )
}

inline fun <reified A> asTypedObjectOrNull(row: ResultRow): Row<A>? {
    val type = row[DataTable.type]
    if (typeTilKlasse[type]?.isSubclassOf(A::class) != true) return null
    val obj = asObject(row)
    return if (obj.data is A) {
        @Suppress("UNCHECKED_CAST")
        obj as Row<A>
    } else {
        null
    }
}
