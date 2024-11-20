package no.nav.paw.aareg.client

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import no.nav.paw.aareg.test.MockResponse
import no.nav.paw.aareg.test.mockAaregClient
import no.nav.paw.aareg.model.Result

class AaregClientTest : FreeSpec({
    /*
    API Description:
    https://aareg-services.intern.dev.nav.no/swagger-ui/index.html?urls.primaryName=aareg.api.v2
    */

    "Returnerer gyldig objekt når alt er ok" {
        val response = runBlocking {
            mockAaregClient(MockResponse.arbeidsforhold)
                .hentArbeidsforhold("ident", "call-id")
        }

        response.shouldBeInstanceOf<Result.Success>()
        response.arbeidsforhold.map { it.arbeidssted.identer[0].ident }.first() shouldBe "910825518"

    }

    "Returnerer error objekt når noe går galt" {
        val response = runBlocking {
            mockAaregClient(MockResponse.error, HttpStatusCode.InternalServerError)
                .hentArbeidsforhold("ident", "call-id")
        }

        response.shouldBeInstanceOf<Result.Failure>()
        response.error.meldinger.first() shouldBe "error"

    }
})
