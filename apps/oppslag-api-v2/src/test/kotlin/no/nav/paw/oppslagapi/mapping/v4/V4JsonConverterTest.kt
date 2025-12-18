package no.nav.paw.oppslagapi.mapping.v4

import com.atlassian.oai.validator.model.Request
import com.atlassian.oai.validator.model.SimpleResponse
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.oppslagapi.data.Row
import no.nav.paw.oppslagapi.data.query.genererTidslinje
import no.nav.paw.oppslagapi.model.v3.AggregertPeriode
import no.nav.paw.oppslagapi.model.v3.Tidslinje
import no.nav.paw.oppslagapi.test.TestData
import no.nav.paw.oppslagapi.test.v4ApiValidator
import no.nav.paw.oppslagapi.utils.objectMapper
import java.util.*

class V4JsonConverterTest : FreeSpec({
    "Skal mappe til riktig typer for snapshot" {
        val rader: List<Pair<UUID, List<Row<Any>>>> = TestData.rows3
            .groupBy { it.periodeId }
            .map { (periodeId, rows) -> periodeId to rows }
        val input = genererTidslinje(rader).first().asV4().asAggregertPeriodeV4()

        val json = objectMapper.writeValueAsString(input)
        println(json)
        val output = objectMapper.readValue<AggregertPeriode>(json)
        println(output)

        val response = SimpleResponse.Builder(
            200
        ).withContentType("application/json")
            .withBody(json.toByteArray().inputStream())
            .build()

        val result = v4ApiValidator
            .validateResponse("/api/v4/snapshot", Request.Method.POST, response)
        withClue(result) { result.hasErrors() shouldBe false }

        println(result)
    }

    "Skal mappe til riktig typer for perioder" {
        val rader: List<Pair<UUID, List<Row<Any>>>> = TestData.rows3
            .groupBy { it.periodeId }
            .map { (periodeId, rows) -> periodeId to rows }
        val tidslinje = genererTidslinje(rader).first().asV4()

        val input = listOf(tidslinje)
        val json = objectMapper.writeValueAsString(input)
        println(json)
        val output = objectMapper.readValue<List<Tidslinje>>(json)
        println(output)

        val response = SimpleResponse.Builder(
            200
        ).withContentType("application/json")
            .withBody(json.toByteArray().inputStream())
            .build()

        val result = v4ApiValidator
            .validateResponse("/api/v4/perioder", Request.Method.POST, response)
        withClue(result) { result.hasErrors() shouldBe false }

        println(result)
    }
})