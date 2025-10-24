package no.nav.paw.ledigestillinger.route

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import no.nav.paw.error.model.ProblemDetails
import no.nav.paw.ledigestillinger.api.models.FinnStillingerRequest
import no.nav.paw.ledigestillinger.api.models.FinnStillingerResponse
import no.nav.paw.ledigestillinger.api.models.Fylke
import no.nav.paw.ledigestillinger.api.models.Paging
import no.nav.paw.ledigestillinger.api.models.Stilling
import no.nav.paw.ledigestillinger.exception.STILLING_IKKE_FUNNET_ERROR_TYPE
import no.nav.paw.ledigestillinger.model.asDto
import no.nav.paw.ledigestillinger.model.asStillingRow
import no.nav.paw.ledigestillinger.test.TestContext
import no.nav.paw.ledigestillinger.test.TestData
import no.nav.paw.ledigestillinger.test.validateAgainstOpenApiSpec
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.*

class StillingRoutesTest : FreeSpec({
    with(TestContext.buildWithDatabase()) {

        beforeSpec {
            setUp()
            mockOAuth2Server.start()

            // GIVEN
            transaction {
                stillingService.lagreStilling(TestData.message1_1.asStillingRow())
                stillingService.lagreStilling(TestData.message1_2.asStillingRow())
                stillingService.lagreStilling(TestData.message2_1.asStillingRow())
                stillingService.lagreStilling(TestData.message2_2.asStillingRow())
            }
        }

        afterSpec {
            tearDown()
            mockOAuth2Server.shutdown()
        }

        "Test suite for hent stilling" - {
            securedTestApplication {
                val client = buildTestClient()
                val token = mockOAuth2Server.issueTokenXToken()

                "Skal hente stilling med uuid" {

                    // GIVEN
                    val uuid = TestData.uuid1_1
                    val stillingRow = TestData.message1_1.asStillingRow()

                    // WHEN
                    val response = client.get("/api/v1/stillinger/$uuid") {
                        bearerAuth(token.serialize())
                    }

                    // THEN
                    response.validateAgainstOpenApiSpec()
                    response.status shouldBe HttpStatusCode.OK
                    val body = response.body<Stilling>()
                    body shouldBe stillingRow.asDto()
                }

                "Skal få 404 hvis stillingen ikke finnes" {

                    // GIVEN
                    val uuid = UUID.randomUUID()

                    // WHEN
                    val response = client.get("/api/v1/stillinger/$uuid") {
                        bearerAuth(token.serialize())
                    }

                    // THEN
                    response.validateAgainstOpenApiSpec()
                    response.status shouldBe HttpStatusCode.NotFound
                    val body = response.body<ProblemDetails>()
                    body.status shouldBe HttpStatusCode.NotFound
                    body.title shouldBe HttpStatusCode.NotFound.description
                    body.type shouldBe STILLING_IKKE_FUNNET_ERROR_TYPE
                }
            }
        }

        "Test suite for finn stillinger" - {
            securedTestApplication {
                val client = buildTestClient()
                val token = mockOAuth2Server.issueTokenXToken()

                "Skal finne stillinger med søk på kategori og fylke" {
                    // GIVEN
                    val request = FinnStillingerRequest(
                        soekeord = emptyList(),
                        kategorier = listOf("2011", "2012"),
                        fylker = listOf(
                            Fylke(
                                fylkesnummer = "57",
                                kommuner = emptyList()
                            )
                        ),
                        paging = Paging(1, 10)
                    )

                    // WHEN
                    val response = client.post("/api/v1/stillinger") {
                        bearerAuth(token.serialize())
                        setJsonBody(request)
                    }

                    // THEN
                    response.validateAgainstOpenApiSpec()
                    response.status shouldBe HttpStatusCode.OK
                    val body = response.body<FinnStillingerResponse>()
                    body.stillinger shouldHaveSize 2
                    val stilling1 = body.stillinger[0]
                    stilling1.uuid shouldBe TestData.uuid2_1
                    stilling1 shouldBe TestData.message2_1.asStillingRow().asDto()
                    val stilling2 = body.stillinger[1]
                    stilling2.uuid shouldBe TestData.uuid2_2
                    stilling2 shouldBe TestData.message2_2.asStillingRow().asDto()
                }

                "Skal finne alle stillinger med tomt søk" {
                    // GIVEN
                    val request = FinnStillingerRequest(
                        soekeord = emptyList(),
                        kategorier = emptyList(),
                        fylker = emptyList(),
                        paging = Paging(1, 10)
                    )

                    // WHEN
                    val response = client.post("/api/v1/stillinger") {
                        bearerAuth(token.serialize())
                        setJsonBody(request)
                    }

                    // THEN
                    response.validateAgainstOpenApiSpec()
                    response.status shouldBe HttpStatusCode.OK
                    val body = response.body<FinnStillingerResponse>()
                    body.stillinger shouldHaveSize 4
                    body.stillinger shouldContainOnly listOf(
                        TestData.message1_1.value.asDto(),
                        TestData.message1_2.value.asDto(),
                        TestData.message2_1.value.asDto(),
                        TestData.message2_2.value.asDto()
                    )
                }
            }
        }
    }
})