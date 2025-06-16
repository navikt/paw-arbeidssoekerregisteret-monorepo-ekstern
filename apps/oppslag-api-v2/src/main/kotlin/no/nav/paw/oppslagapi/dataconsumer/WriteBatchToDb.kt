package no.nav.paw.oppslagapi.dataconsumer

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import no.nav.paw.oppslagapi.DataTable
import no.nav.paw.oppslagapi.Row
import no.nav.paw.oppslagapi.appLogger
import org.jetbrains.exposed.sql.batchInsert

fun writeBatchToDb(rows: Sequence<Pair<Row<String>, Span>>) {
    val tracer = GlobalOpenTelemetry
        .getTracer("write_batch_to_db")
    val batchSpan = tracer.spanBuilder("write_batch_to_db")
        .setSpanKind(SpanKind.SERVER)
        .startSpan()
    try {
        val linkedRows = rows
            .map { (row, span) ->
                val span = span.addLink(batchSpan.spanContext)
                try {
                    row
                } finally {
                    span.end()
                }
            }
        val rowCount = DataTable.batchInsert(
            data = linkedRows,
            ignore = false,
            shouldReturnGeneratedValues = false
        ) { row ->
            this[DataTable.type] = row.type
            this[DataTable.identitetsnummer] = row.identitetsnummer
            this[DataTable.periodeId] = row.periodeId
            this[DataTable.timestamp] = row.timestamp
            this[DataTable.data] = row.data
        }.count()
        appLogger.debug("Skrev $rowCount rader til databasen")
    } finally {
        batchSpan.end()
    }
}