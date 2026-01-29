package no.nav.paw.arbeidssoekerregisteret.utils

import com.fasterxml.jackson.databind.MappingIterator
import com.fasterxml.jackson.databind.ObjectReader
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.nav.paw.arbeidssoekerregisteret.model.IdentitetsnummerRow
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.KClass

abstract class CsvReader<T : Any>(
    columns: Map<String, CsvSchema.ColumnType>,
    kClass: KClass<T>,
    columnSeparator: Char = ',',
    hasHeader: Boolean = false,
    hasComments: Boolean = false
) {
    private val csvReader: ObjectReader = buildCsvReader(
        csvSchema = buildCsvSchema(
            columns = columns,
            columnSeparator = columnSeparator,
            hasHeader = hasHeader,
            hasComments = hasComments
        ),
        kClass = kClass
    );

    fun readValues(uri: URI): MappingIterator<T> = csvReader.readValues(uri.toURL().openStream())
    fun readValues(path: Path): MappingIterator<T> {
        if (!Files.exists(path)) {
            throw NoSuchFileException(file = path.toFile(), reason = "Ingen fil funnet for sti $path")
        }
        if (!Files.isRegularFile(path)) {
            throw NoSuchFileException(file = path.toFile(), reason = "Sti $path er ikke en fil")
        }
        if (!Files.isReadable(path)) {
            throw AccessDeniedException(file = path.toFile(), reason = "$path kan ikke leses fra")
        }
        return readValues(path.toUri())
    }

    private fun buildCsvReader(
        csvSchema: CsvSchema,
        kClass: KClass<T>
    ): ObjectReader {
        return CsvMapper()
            .registerModule(KotlinModule.Builder().build())
            .registerModule(JavaTimeModule())
            .readerFor(kClass.java)
            .with(csvSchema)
    }

    private fun buildCsvSchema(
        columns: Map<String, CsvSchema.ColumnType>,
        columnSeparator: Char,
        hasHeader: Boolean,
        hasComments: Boolean
    ): CsvSchema {
        val builder = CsvSchema.builder()
            .setColumnSeparator(columnSeparator)
            .setUseHeader(hasHeader)
            .setAllowComments(hasComments)
        columns.forEach { (name, type) ->
            builder.addColumn(name, type)
        }
        return builder.build()
    }
}

object IdentitetsnummerCsvReader : CsvReader<IdentitetsnummerRow>(
    columns = mapOf("identitetsnummer" to CsvSchema.ColumnType.STRING),
    kClass = IdentitetsnummerRow::class
)