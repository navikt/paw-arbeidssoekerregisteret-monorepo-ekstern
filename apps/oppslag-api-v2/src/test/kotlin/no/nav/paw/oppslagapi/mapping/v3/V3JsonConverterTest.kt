package no.nav.paw.oppslagapi.mapping.v3

import com.atlassian.oai.validator.model.Request
import com.atlassian.oai.validator.model.SimpleResponse
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.oppslagapi.data.objectMapper
import no.nav.paw.oppslagapi.model.v3.Bruker
import no.nav.paw.oppslagapi.model.v3.BrukerType
import no.nav.paw.oppslagapi.model.v3.PeriodeStartet
import no.nav.paw.oppslagapi.model.v3.Tidslinje
import no.nav.paw.oppslagapi.test.v3ApiValidator
import java.time.Instant
import java.util.*

class V3JsonConverterTest : FreeSpec({
    "Skal mappe til riktig type".config(enabled = false) {
        val periodeStartet = PeriodeStartet(
            tidspunkt = Instant.now(),
            utfoertAv = Bruker(id = "1234", type = BrukerType.SLUTTBRUKER),
            kilde = "kilde",
            aarsak = "aarsak",
        )
        val tidslinje = Tidslinje(
            periodeId = UUID.randomUUID(),
            identitetsnummer = "1234",
            startet = Instant.now(),
            avsluttet = null,
            hendelser = listOf(periodeStartet),
        )
        val tidslinjer = listOf(tidslinje)
        val json = objectMapper.writeValueAsString(tidslinjer)
        println(json)
        val hendelser = objectMapper.readValue<List<Tidslinje>>(json)
        println(hendelser)

        val response = SimpleResponse.Builder(
            200
        ).withContentType("application/json")
            .withBody(json.toByteArray().inputStream())
            .build()

        val result = v3ApiValidator
            .validateResponse("/api/v3/tidslinjer", Request.Method.POST, response)
        withClue(result) { result.hasErrors() shouldBe false }

        println(result)
    }
})