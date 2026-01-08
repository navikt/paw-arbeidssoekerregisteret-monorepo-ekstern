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
import no.nav.paw.ledigestillinger.exception.STILLING_IKKE_FUNNET_ERROR_TYPE
import no.nav.paw.ledigestillinger.model.asDto
import no.nav.paw.ledigestillinger.model.asStillingRow
import no.nav.paw.ledigestillinger.test.TestContext
import no.nav.paw.ledigestillinger.test.TestData
import no.nav.paw.ledigestillinger.test.validateAgainstOpenApiSpec
import no.naw.paw.ledigestillinger.model.FinnStillingerByEgenskaperRequest
import no.naw.paw.ledigestillinger.model.FinnStillingerByUuidListeRequest
import no.naw.paw.ledigestillinger.model.FinnStillingerResponse
import no.naw.paw.ledigestillinger.model.FinnStillingerType
import no.naw.paw.ledigestillinger.model.Fylke
import no.naw.paw.ledigestillinger.model.Kommune
import no.naw.paw.ledigestillinger.model.Paging
import no.naw.paw.ledigestillinger.model.Stilling
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
                stillingService.lagreStilling(TestData.message3_1.asStillingRow())
                stillingService.lagreStilling(TestData.message3_2.asStillingRow())
                stillingService.lagreStilling(TestData.message4_1.asStillingRow())
                stillingService.lagreStilling(TestData.message4_2.asStillingRow())
                stillingService.lagreStilling(TestData.message5_1.asStillingRow())
                stillingService.lagreStilling(TestData.message5_2.asStillingRow())
                stillingService.lagreStilling(TestData.message5_3.asStillingRow())
                stillingService.lagreStilling(TestData.message5_4.asStillingRow())
                stillingService.lagreStilling(TestData.message5_5.asStillingRow())
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

        "Test suite for finn stillinger for egenskaper" - {
            securedTestApplication {
                val client = buildTestClient()
                val token = mockOAuth2Server.issueTokenXToken()

                "Skal finne stillinger med søk på kategori og fylke" {
                    // GIVEN
                    val request = FinnStillingerByEgenskaperRequest(
                        type = FinnStillingerType.BY_EGENSKAPER,
                        soekeord = emptyList(),
                        styrkkoder = listOf("2012"),
                        fylker = listOf(
                            Fylke(
                                fylkesnummer = "20",
                                kommuner = emptyList()
                            )
                        ),
                        paging = Paging(1, 100)
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
                    body.stillinger shouldHaveSize 1
                    body.stillinger shouldContainOnly listOf(
                        TestData.message2_2.value.asDto()
                    )
                }

                "Skal finne stillinger med søk på fylke" {
                    // GIVEN
                    val request = FinnStillingerByEgenskaperRequest(
                        type = FinnStillingerType.BY_EGENSKAPER,
                        soekeord = emptyList(),
                        styrkkoder = emptyList(),
                        fylker = listOf(
                            Fylke(
                                fylkesnummer = "30",
                                kommuner = emptyList()
                            )
                        ),
                        paging = Paging(1, 100)
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
                    body.stillinger shouldContainOnly listOf(
                        TestData.message3_1.value.asDto(),
                        TestData.message3_2.value.asDto()
                    )
                }

                "Skal finne stillinger med søk på fylke og kommune" {
                    // GIVEN
                    val request = FinnStillingerByEgenskaperRequest(
                        type = FinnStillingerType.BY_EGENSKAPER,
                        soekeord = emptyList(),
                        styrkkoder = emptyList(),
                        fylker = listOf(
                            Fylke(
                                fylkesnummer = "30",
                                kommuner = listOf(
                                    Kommune(
                                        kommunenummer = "3012"
                                    )
                                )
                            )
                        ),
                        paging = Paging(1, 100)
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
                    body.stillinger shouldHaveSize 1
                    body.stillinger shouldContainOnly listOf(
                        TestData.message3_2.value.asDto()
                    )
                }

                "Skal finne stillinger med søk på kategori" {
                    // GIVEN
                    val request = FinnStillingerByEgenskaperRequest(
                        type = FinnStillingerType.BY_EGENSKAPER,
                        soekeord = emptyList(),
                        styrkkoder = listOf("4011", "4012"),
                        fylker = emptyList(),
                        paging = Paging(1, 100)
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
                    body.stillinger shouldContainOnly listOf(
                        TestData.message4_1.value.asDto(),
                        TestData.message4_2.value.asDto()
                    )
                }

                "Skal finne alle stillinger med tomt søk" {
                    // GIVEN
                    val request = FinnStillingerByEgenskaperRequest(
                        type = FinnStillingerType.BY_EGENSKAPER,
                        soekeord = emptyList(),
                        styrkkoder = emptyList(),
                        fylker = emptyList(),
                        paging = Paging(1, 100)
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
                    body.stillinger shouldHaveSize 11
                    body.stillinger shouldContainOnly listOf(
                        TestData.message1_1.value.asDto(),
                        TestData.message1_2.value.asDto(),
                        TestData.message2_1.value.asDto(),
                        TestData.message2_2.value.asDto(),
                        TestData.message3_1.value.asDto(),
                        TestData.message3_2.value.asDto(),
                        TestData.message4_1.value.asDto(),
                        TestData.message4_2.value.asDto(),
                        TestData.message5_1.value.asDto(),
                        TestData.message5_2.value.asDto(),
                        TestData.message5_3.value.asDto()
                    )
                }

                "Skal ikke finne stillinger med kilde 'DIR'" {
                    // GIVEN
                    val request = FinnStillingerByEgenskaperRequest(
                        type = FinnStillingerType.BY_EGENSKAPER,
                        soekeord = emptyList(),
                        styrkkoder = emptyList(),
                        fylker = listOf(
                            Fylke(
                                fylkesnummer = "50",
                                kommuner = emptyList()
                            )
                        ),
                        paging = Paging(1, 100)
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
                    body.stillinger shouldHaveSize 3
                    body.stillinger shouldContainOnly listOf(
                        TestData.message5_1.value.asDto(),
                        TestData.message5_2.value.asDto(),
                        TestData.message5_3.value.asDto()
                    )
                }
            }
        }

        "Test suite for finn stillinger for uuid-liste" - {
            securedTestApplication {
                val client = buildTestClient()
                val token = mockOAuth2Server.issueTokenXToken()

                "Skal finne stillinger for uuid-liste" {
                    // GIVEN
                    val request = FinnStillingerByUuidListeRequest(
                        type = FinnStillingerType.BY_UUID_LISTE,
                        uuidListe = listOf(TestData.uuid1_2, TestData.uuid3_1, TestData.uuid4_2, UUID.randomUUID())
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
                    body.stillinger shouldHaveSize 3
                    body.stillinger shouldContainOnly listOf(
                        TestData.message1_2.value.asDto(),
                        TestData.message3_1.value.asDto(),
                        TestData.message4_2.value.asDto()
                    )
                }
            }
        }
    }
})