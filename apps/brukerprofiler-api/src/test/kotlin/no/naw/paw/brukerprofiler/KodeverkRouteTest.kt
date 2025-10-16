package no.naw.paw.brukerprofiler

import io.kotest.core.spec.style.FreeSpec
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
                    fylke.kommuner shouldContain "Bergen"
                    fylke.kommuner shouldContain "Stord"
                    fylke.kommuner shouldContain "Askøy"
                }
                fylker
                    .firstOrNull { it.navn.equals("Troms") } should { fylke ->
                    fylke.shouldNotBeNull()

                    fylke.kommuner shouldContain "Tromsø"
                }
            }
        }
    }
})

