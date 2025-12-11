package no.nav.paw.oppslagapi.mapping.v3

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
import no.nav.paw.oppslagapi.test.v3ApiValidator
import no.nav.paw.oppslagapi.utils.objectMapper
import java.util.*

class V3JsonConverterTest : FreeSpec({
    "Skal mappe til riktig typer for snapshot" {
        val rader: List<Pair<UUID, List<Row<Any>>>> = TestData.rows3
            .groupBy { it.periodeId }
            .map { (periodeId, rows) -> periodeId to rows }
        val input = genererTidslinje(rader).first().asV3().asAggregertPeriode()

        val json = objectMapper.writeValueAsString(input)
        objectMapper.readValue<AggregertPeriode>(json)

        val response = SimpleResponse.Builder(
            200
        ).withContentType("application/json")
            .withBody(json.toByteArray().inputStream())
            .build()

        val result = v3ApiValidator
            .validateResponse("/api/v3/snapshot", Request.Method.POST, response)
        withClue(result) { result.hasErrors() shouldBe false }
    }

    "Skal mappe til riktig typer for perioder" {
        val rader: List<Pair<UUID, List<Row<Any>>>> = TestData.rows3
            .groupBy { it.periodeId }
            .map { (periodeId, rows) -> periodeId to rows }
        val tidslinje = genererTidslinje(rader).first().asV3()

        val input = listOf(tidslinje)
        val json = objectMapper.writeValueAsString(input)
        objectMapper.readValue<List<Tidslinje>>(json)

        val response = SimpleResponse.Builder(
            200
        ).withContentType("application/json")
            .withBody(json.toByteArray().inputStream())
            .build()

        val result = v3ApiValidator
            .validateResponse("/api/v3/perioder", Request.Method.POST, response)
        withClue(result) { result.hasErrors() shouldBe false }
    }
})