package no.nav.paw.oppslagapi.data.query

import no.nav.paw.oppslagapi.data.DataTable
import no.nav.paw.oppslagapi.data.Row
import no.nav.paw.oppslagapi.data.objectMapper
import no.nav.paw.oppslagapi.data.typeTilKlasse
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

interface DatabaseQeurySupport {
    fun hentRaderForPeriode(
        periodeId: UUID
    ): List<Row<Any>>
}

private object ExposedDatabaseQuerySupport : DatabaseQeurySupport {
    override fun hentRaderForPeriode(
        periodeId: UUID
    ): List<Row<Any>> =
        transaction {
            DataTable.selectAll()
                .where { DataTable.periodeId eq periodeId }
                .map(::asObject)
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