package no.nav.paw.serialization.kontrakt

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import no.nav.paw.client.factory.createObjectMapper
import no.nav.paw.error.model.ProblemDetails
import no.nav.paw.serialization.jackson.buildObjectMapper
import java.net.URI

/**
 * JSON-kontrakt for [ProblemDetails] slik den sendes ut av server-mapperen
 * ([buildObjectMapper], nulls tas med) og klient-mapperen ([createObjectMapper], NON_NULL).
 *
 * Fasitfilene under `json-kontrakt/problem-details/` er generert fra produksjonskoden og
 * må aldri endres. Feiler testen er kontrakten mot konsumentene brutt.
 */
class ProblemDetailsJsonKontraktTest : FreeSpec({

    val utenDetail = ProblemDetails(
        id = FAST_ID,
        type = URI("urn:paw:default:ukjent-feil"),
        status = HttpStatusCode.InternalServerError,
        title = "Internal Server Error",
        detail = null,
        instance = "/api/v1/noe",
        timestamp = FAST_TIDSPUNKT,
    )
    val medDetail = utenDetail.copy(
        type = URI("urn:paw:http:kunne-ikke-tolke-forespoersel"),
        status = HttpStatusCode.BadRequest,
        title = "Bad Request",
        detail = "Kunne ikke tolke forespørsel",
    )

    fun sjekkKontrakt(mapper: ObjectMapper, verdi: ProblemDetails, fasitfil: String) {
        val fasit = lesFasit("problem-details/$fasitfil")
        mapper.writeValueAsString(verdi) skalVaereLikFasit fasit
        mapper.readValue<ProblemDetails>(fasit) shouldBe verdi
    }

    "Server-mapper (buildObjectMapper)" - {
        "ProblemDetails uten detail skrives med detail=null" {
            sjekkKontrakt(buildObjectMapper, utenDetail, "server-mapper-uten-detail.json")
        }
        "ProblemDetails med detail" {
            sjekkKontrakt(buildObjectMapper, medDetail, "server-mapper-med-detail.json")
        }
    }

    "Klient-mapper (createObjectMapper)" - {
        "ProblemDetails uten detail utelater detail-feltet" {
            sjekkKontrakt(createObjectMapper(), utenDetail, "klient-mapper-uten-detail.json")
        }
        "ProblemDetails med detail" {
            sjekkKontrakt(createObjectMapper(), medDetail, "klient-mapper-med-detail.json")
        }
    }
})
