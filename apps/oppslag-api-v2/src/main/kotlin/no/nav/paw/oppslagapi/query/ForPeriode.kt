package no.nav.paw.oppslagapi.query

import no.nav.paw.oppslagapi.DataTable
import no.nav.paw.oppslagapi.Row
import no.nav.paw.oppslagapi.objectMapper
import no.nav.paw.oppslagapi.typeTilKlasse
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.selectAll
import java.util.*

fun Transaction.hentForPeriode(
    periodeId: UUID
): List<Row<Any>> {
    val result = DataTable.selectAll()
        .where { DataTable.periodeId eq periodeId }
        .map(::asObject)
    return result
}

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
