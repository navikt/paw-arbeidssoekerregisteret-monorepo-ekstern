package no.nav.paw.arbeidssoekerregisteret

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType.Application
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.api.models.EgenvurderingGrunnlag
import no.nav.paw.arbeidssoekerregisteret.routes.egenvurderingGrunnlagPath
import no.nav.paw.arbeidssoekerregisteret.routes.egenvurderingPath
import java.util.*

class EgenvurderingRoutesTest : FreeSpec({
    with(TestContext()) {
        beforeSpec {
            mockOAuth2Server.start()
        }

        afterSpec {
            mockOAuth2Server.shutdown()
        }
        "/api/v1/arbeidssoeker/profilering/egenvurdering" - {
            "202 Accepted" - {
                testApplication {
                    configureTestApplication()
                    val client = configureTestClient()
                    val egenvurderingJson = """
                            {
                              "profileringId": "${UUID.randomUUID()}",
                              "egenvurdering": "ANTATT_GODE_MULIGHETER"
                            }
                        """.trimIndent()

                    val response = client.post(egenvurderingPath) {
                        contentType(Application.Json)
                        setBody(egenvurderingJson)
                        bearerAuth(mockOAuth2Server.issueTokenXToken())
                    }
                    response.status shouldBe HttpStatusCode.Accepted
                    response.headers["x-trace-id"] shouldNotBe null
                }
            }
            "400 BadRequest" - {
                testApplication {
                    configureTestApplication()
                    val client = configureTestClient()

                    val response = client.post(egenvurderingPath) {
                        contentType(Application.Json)
                        setBody("""{ugyldigJson}""")
                        bearerAuth(mockOAuth2Server.issueTokenXToken())
                    }
                    response.status shouldBe HttpStatusCode.BadRequest
                    response.headers["x-trace-id"] shouldNotBe null
                }
            }
        }

        "/api/v1/arbeidssoeker/profilering/egenvurdering/grunnlag" - {
            "200 OK" - {
                testApplication {
                    configureTestApplication()

                    val client = configureTestClient()

                    val response = client.get(egenvurderingGrunnlagPath) {
                        bearerAuth(mockOAuth2Server.issueTokenXToken())
                    }
                    response.status shouldBe HttpStatusCode.OK
                    response.body<EgenvurderingGrunnlag>() shouldBe EgenvurderingGrunnlag(grunnlag = null)
                    response.bodyAsText() shouldBe """{}"""
                    response.headers["x-trace-id"] shouldNotBe null
                }
            }
            "403 Forbidden" - {
                testApplication {
                    configureTestApplication()

                    val client = configureTestClient()

                    val response = client.get(egenvurderingGrunnlagPath)
                    response.status shouldBe HttpStatusCode.Forbidden
                    response.headers["x-trace-id"] shouldNotBe null
                }
            }
        }
    }
})