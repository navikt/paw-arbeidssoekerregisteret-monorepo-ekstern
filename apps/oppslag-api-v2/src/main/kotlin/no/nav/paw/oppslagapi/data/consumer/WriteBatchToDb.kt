package no.nav.paw.oppslagapi.data.consumer

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import no.nav.paw.logging.logger.buildNamedLogger
import no.nav.paw.oppslagapi.data.DataTable
import no.nav.paw.oppslagapi.data.Row
import org.jetbrains.exposed.v1.jdbc.batchInsert

private val logger = buildNamedLogger("database")

fun writeBatchToDb(rows: Sequence<Pair<Row<String>, Span>>) {
    val tracer = GlobalOpenTelemetry
        .getTracer("write_batch_to_db")
    val batchSpan = tracer.spanBuilder("write_batch_to_db")
        .setSpanKind(SpanKind.INTERNAL)
        .startSpan()
    runCatching {
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
        logger.debug("Skrev $rowCount rader til databasen")
        batchSpan.setAttribute("db.rows_inserted", rowCount.toLong())
    }
        .onSuccess { batchSpan.end() }
        .onFailure { t ->
            batchSpan.recordException(t)
            batchSpan.setAttribute("error.type", t::class.java.canonicalName)
            batchSpan.setStatus(StatusCode.ERROR, t.message ?: "Feil ved skriving til database")
            batchSpan.end()
        }.getOrThrow()
}