package no.nav.paw.oppslagapi

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.paw.error.model.onFailure
import no.nav.paw.error.model.onSuccess
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.logging.logger.AuditLogger
import no.nav.paw.oppslagapi.exception.FinnIdentiteterFeiletException
import no.nav.paw.oppslagapi.test.TestContext
import no.nav.paw.oppslagapi.test.TestData
import no.nav.paw.oppslagapi.test.configureMock
import no.nav.paw.security.authentication.model.AzureAd
import no.nav.paw.security.authentication.model.Claims
import no.nav.paw.security.authentication.model.Roles
import no.nav.paw.security.authentication.model.SecurityContext
import no.nav.paw.security.authentication.model.TokenX
import no.nav.paw.security.authentication.token.AccessToken
import no.nav.paw.tilgangskontroll.client.TilgangsTjenesteForAnsatte
import org.junit.jupiter.api.fail
import java.net.URI

class AutoriseringTest : FreeSpec({
    val kafkaKeysClientMock: KafkaKeysClient = mockk()
    val tilgangsTjenesteForAnsatteMock: TilgangsTjenesteForAnsatte = mockk()
    with(
        TestContext(
            autorisasjonsTjeneste = AutorisasjonsTjeneste(
                kafkaKeysClient = kafkaKeysClientMock,
                tilgangsTjenesteForAnsatte = tilgangsTjenesteForAnsatteMock,
                auditLogger = AuditLogger.getLogger()
            ),
        )
    ) {
        beforeSpec {
            tilgangsTjenesteForAnsatteMock.configureMock()
            kafkaKeysClientMock.configureMock()
            coEvery { kafkaKeysClientMock.getIdentiteter(identitetsnummer = TestData.fnr6.value) } throws IllegalStateException(
                "There be monsters"
            )
        }

        "Skal autorisere anonym" {
            val securityContext1 = SecurityContext(
                bruker = TestData.anonym1,
                accessToken = AccessToken(
                    jwt = "whatever",
                    issuer = AzureAd,
                    claims = Claims(mapOf(Roles to listOf("access_as_application")))
                )
            )
            val securityContext2 = SecurityContext(
                bruker = TestData.anonym1,
                accessToken = AccessToken(
                    jwt = "whatever",
                    issuer = AzureAd,
                    claims = Claims(emptyMap())
                )
            )

            val response1 = autorisasjonsTjeneste.autoriser(
                handling = "test",
                securityContext = securityContext1,
                oenskerTilgangTil = listOf(
                    TestData.fnr1
                )
            ) {
                "OK 1"
            }
            val response2 = autorisasjonsTjeneste.autoriser(
                handling = "test",
                securityContext = securityContext2,
                oenskerTilgangTil = listOf(
                    TestData.fnr1
                )
            ) {
                "IKKE OK 2"
            }

            response1
                .onFailure { _ -> fail { "Skal ikke feile" } }
                .onSuccess { resultat -> resultat shouldBe "OK 1" }
            response2
                .onFailure { error ->
                    error.status shouldBe HttpStatusCode.Forbidden
                    error.type shouldBe URI.create("urn:paw:security:ingen-tilgang")
                }
                .onSuccess { _ -> fail { "Skal feile" } }
        }

        "Skal autorisere ansatt" {
            val securityContext1 = SecurityContext(
                bruker = TestData.ansatt1,
                accessToken = AccessToken(
                    jwt = "whatever",
                    issuer = AzureAd,
                    claims = Claims(emptyMap())
                )
            )
            val securityContext3 = SecurityContext(
                bruker = TestData.ansatt3,
                accessToken = AccessToken(
                    jwt = "whatever",
                    issuer = AzureAd,
                    claims = Claims(emptyMap())
                )
            )

            val response11 = autorisasjonsTjeneste.autoriser(
                handling = "test",
                securityContext = securityContext1,
                oenskerTilgangTil = listOf(
                    TestData.fnr1
                )
            ) {
                "OK 1 1"
            }
            val response12 = autorisasjonsTjeneste.autoriser(
                handling = "test",
                securityContext = securityContext1,
                oenskerTilgangTil = listOf(
                    TestData.fnr2
                )
            ) {
                "OK 1 2"
            }
            val response31 = autorisasjonsTjeneste.autoriser(
                handling = "test",
                securityContext = securityContext3,
                oenskerTilgangTil = listOf(
                    TestData.fnr1
                )
            ) {
                "IKKE OK"
            }
            val response32 = autorisasjonsTjeneste.autoriser(
                handling = "test",
                securityContext = securityContext3,
                oenskerTilgangTil = listOf(
                    TestData.fnr2
                )
            ) {
                "OK 3 2"
            }

            response11
                .onFailure { _ -> fail { "Skal ikke feile" } }
                .onSuccess { resultat -> resultat shouldBe "OK 1 1" }
            response12
                .onFailure { _ -> fail { "Skal ikke feile" } }
                .onSuccess { resultat -> resultat shouldBe "OK 1 2" }
            response31
                .onFailure { error ->
                    error.status shouldBe HttpStatusCode.Forbidden
                    error.type shouldBe URI.create("urn:paw:tilgangskontroll:ikke_tilgang")
                }
                .onSuccess { _ -> fail { "Skal feile" } }
            response32
                .onFailure { _ -> fail { "Skal ikke feile" } }
                .onSuccess { resultat -> resultat shouldBe "OK 3 2" }
        }

        "Skal autorisere sluttbruker" {
            val securityContext1 = SecurityContext(
                bruker = TestData.bruker1,
                accessToken = AccessToken(
                    jwt = "whatever",
                    issuer = TokenX,
                    claims = Claims(emptyMap())
                )
            )
            val securityContext2 = SecurityContext(
                bruker = TestData.bruker2,
                accessToken = AccessToken(
                    jwt = "whatever",
                    issuer = TokenX,
                    claims = Claims(emptyMap())
                )
            )
            val securityContext6 = SecurityContext(
                bruker = TestData.bruker6,
                accessToken = AccessToken(
                    jwt = "whatever",
                    issuer = TokenX,
                    claims = Claims(emptyMap())
                )
            )

            val response11 = autorisasjonsTjeneste.autoriser(
                handling = "test",
                securityContext = securityContext1,
                oenskerTilgangTil = listOf(
                    TestData.fnr1
                )
            ) {
                "OK 1 1"
            }
            val response12 = autorisasjonsTjeneste.autoriser(
                handling = "test",
                securityContext = securityContext1,
                oenskerTilgangTil = listOf(
                    TestData.fnr2
                )
            ) {
                "IKKE OK 1 2"
            }
            val response21 = autorisasjonsTjeneste.autoriser(
                handling = "test",
                securityContext = securityContext2,
                oenskerTilgangTil = listOf(
                    TestData.fnr1
                )
            ) {
                "IKKE OK 2 1"
            }
            val response22 = autorisasjonsTjeneste.autoriser(
                handling = "test",
                securityContext = securityContext2,
                oenskerTilgangTil = listOf(
                    TestData.fnr2
                )
            ) {
                "OK 2 2"
            }

            val exception = shouldThrow<FinnIdentiteterFeiletException> {
                autorisasjonsTjeneste.autoriser(
                    handling = "test",
                    securityContext = securityContext6,
                    oenskerTilgangTil = listOf(
                        TestData.fnr6
                    )
                ) {
                    "OK 6"
                }
            }

            response11
                .onFailure { _ -> fail { "Skal ikke feile" } }
                .onSuccess { resultat -> resultat shouldBe "OK 1 1" }
            response12
                .onFailure { error ->
                    error.status shouldBe HttpStatusCode.Forbidden
                    error.type shouldBe URI.create("urn:paw:security:ingen-tilgang")
                }
                .onSuccess { _ -> fail { "Skal feile" } }
            response21
                .onFailure { error ->
                    error.status shouldBe HttpStatusCode.Forbidden
                    error.type shouldBe URI.create("urn:paw:security:ingen-tilgang")
                }
                .onSuccess { _ -> fail { "Skal feile" } }
            response22
                .onFailure { _ -> fail { "Skal ikke feile" } }
                .onSuccess { resultat -> resultat shouldBe "OK 2 2" }
            val illegalStateException = exception.cause.shouldBeInstanceOf<IllegalStateException>()
            illegalStateException.message shouldBe "There be monsters"
        }
    }
})