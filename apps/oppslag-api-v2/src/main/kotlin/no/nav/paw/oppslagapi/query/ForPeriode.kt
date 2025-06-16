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
): List<Row<String>> {
    val result = DataTable.selectAll()
        .where { DataTable.periodeId eq periodeId }
        .map { resultRow ->
            Row(
                type = resultRow[DataTable.type],
                identitetsnummer = resultRow[DataTable.identitetsnummer],
                periodeId = resultRow[DataTable.periodeId],
                timestamp = resultRow[DataTable.timestamp],
                data = resultRow[DataTable.data]
            )
        }
    return result
}

fun ResultRow.asObject(): Row<Any> {
    val type = get(DataTable.type)
    val clazz = typeTilKlasse[type]?.java
        ?: throw IllegalArgumentException("Ukjent type: $type")
    return Row(
        type = type,
        identitetsnummer = get(DataTable.identitetsnummer),
        periodeId = get(DataTable.periodeId),
        timestamp = get(DataTable.timestamp),
        data = objectMapper.readValue(get(DataTable.data), clazz)
    )
}
