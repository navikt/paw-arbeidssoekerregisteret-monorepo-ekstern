package no.nav.paw.arbeidssoekerregisteret.utils

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainOnly
import kotlin.io.path.toPath

class IdentitetsnummerCsvReaderTest : FreeSpec({
    "Skal lese identiteter fra CSV-fil" {
        val filePath = javaClass.getResource("/data/identitetsnummer.csv")?.toURI()?.toPath()!!
        var fileRows = IdentitetsnummerCsvReader.readValues(filePath)
        val identitetsnummer = fileRows.asSequence().toList().map { it.identitetsnummer }
        identitetsnummer shouldContainOnly listOf(
            "01017012345",
            "02017012345",
            "03017012345",
            "04017012345",
            "05017012345"
        )
    }
})