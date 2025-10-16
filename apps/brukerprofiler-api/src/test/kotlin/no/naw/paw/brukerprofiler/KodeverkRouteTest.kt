package no.naw.paw.brukerprofiler

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.ktor.server.routing.route
import io.ktor.server.testing.testApplication
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.model.Identitetsnummer
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.naw.paw.brukerprofiler.api.vo.ApiStyrk
import no.naw.paw.brukerprofiler.api.vo.StyrkTreNode
import no.naw.paw.brukerprofiler.domain.Fylke

class KodeverkRouteTest : FreeSpec({
    val oauthServer = MockOAuth2Server()
    beforeSpec {
        oauthServer.start()
    }
    afterSpec { oauthServer.shutdown() }


    "Verifiser henting av kodeverk" - {
        testApplication {
            application {
                configureKtorServer(
                    prometheusRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
                    meterBinders = emptyList(),
                    authProviders = listOf(oauthServer.tokenXAuthProvider)
                )
            }
            routing { route("/api/v1") { kodeverk() } }
            val testIdent = Identitetsnummer("12345678900")
            val client = testClient()
            "Vi kan hente en liste med alle fylker og kommuner" {
                val (httpStatus, fylker) = client.get<List<Fylke>>(
                    oauthServer.sluttbrukerTokenJwt(testIdent),
                    "/api/v1/kodeverk/fylker"
                )
                httpStatus.value shouldBe 200
                fylker.size shouldBe 15
                fylker
                    .firstOrNull { it.navn.equals("Vestland") } should { fylke ->
                    fylke.shouldNotBeNull()
                    fylke.kommuner.map { it.navn } shouldContain "Bergen"
                    fylke.kommuner.map { it.navn } shouldContain "Stord"
                    fylke.kommuner.map { it.navn } shouldContain "Askøy"
                }
                fylker
                    .firstOrNull { it.navn.equals("Troms") } should { fylke ->
                    fylke.shouldNotBeNull()

                    fylke.kommuner.map { it.navn } shouldContain "Tromsø"
                }
            }
            "Vi kan hente en liste med alle stillingskoder (STYRK08)" {
                val (httpStatus, styrk) = client.get<List<StyrkTreNode>>(
                    oauthServer.sluttbrukerTokenJwt(testIdent),
                    "/api/v1/kodeverk/styrk08"
                )
                httpStatus.value shouldBe 200
                styrk.find { it.styrk.code == "2" } should { akademiskeYrker ->
                    akademiskeYrker.shouldNotBeNull()
                    akademiskeYrker.styrk.code shouldBe "2"
                    akademiskeYrker.styrk.name shouldBe "Akademiske yrker"
                    akademiskeYrker.styrk.level shouldBe "1"
                    akademiskeYrker.children.find { it.styrk.code == "22" } should { medisinskeYrker ->
                        medisinskeYrker.shouldNotBeNull()
                        medisinskeYrker.styrk.code shouldBe "22"
                        medisinskeYrker.styrk.name shouldBe "Medisinske yrker"
                        medisinskeYrker.styrk.level shouldBe "2"
                        medisinskeYrker.children.find { it.styrk.code == "222" } should { sykepleier ->
                            sykepleier.shouldNotBeNull()
                            sykepleier.styrk.code shouldBe "222"
                            sykepleier.styrk.name shouldBe "Sykepleiere og spesialsykepleiere"
                            sykepleier.styrk.level shouldBe "3"
                            sykepleier.children.find { it.styrk.code == "2223" } should { spesialsykepleier ->
                                spesialsykepleier.shouldNotBeNull()
                                spesialsykepleier.styrk.code shouldBe "2223"
                                spesialsykepleier.styrk.name shouldBe "Sykepleiere"
                                spesialsykepleier.styrk.level shouldBe "4"
                                spesialsykepleier.children.shouldBeEmpty()
                            }
                        }
                    }
                }
            }
        }
    }
})

