package no.nav.paw.arbeidssoekerregisteret.api.oppslag.services

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.verify
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.ApplicationTestContext
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.TestData
import no.nav.paw.pdl.graphql.generated.enums.IdentGruppe
import no.nav.paw.pdl.graphql.generated.hentidenter.IdentInformasjon
import no.nav.paw.security.authentication.model.Identitetsnummer
import no.nav.poao_tilgang.client.Decision
import no.nav.poao_tilgang.client.PolicyRequest
import no.nav.poao_tilgang.client.PolicyResult
import java.util.*

class AutorisasjonServiceTest : FreeSpec({
    with(ApplicationTestContext.withMockDataAccess()) {

        afterSpec {
            confirmVerified(
                pdlHttpConsumerMock,
                poaoTilgangHttpConsumerMock
            )
        }

        "Skal finne identer for fnr" {
            val identer: List<IdentInformasjon> = listOf(
                IdentInformasjon("01017012345", IdentGruppe.FOLKEREGISTERIDENT),
                IdentInformasjon("12345", IdentGruppe.NPID),
                IdentInformasjon("23456", IdentGruppe.NPID),
                IdentInformasjon("98765", IdentGruppe.AKTORID),
                IdentInformasjon("02017012345", IdentGruppe.FOLKEREGISTERIDENT),
                IdentInformasjon("87654", IdentGruppe.__UNKNOWN_VALUE)
            )
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns identer

            val identiteter = authorizationService.finnIdentiteter(TestData.identitetsnummer1)

            identiteter shouldHaveSize 2
            identiteter shouldContain Identitetsnummer("01017012345")
            identiteter shouldContain Identitetsnummer("02017012345")
            coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
        }

        "verifiserTilgangTilBruker should return true if access is granted" {
            every {
                poaoTilgangHttpConsumerMock.evaluatePolicies(any<List<PolicyRequest>>())
            } returns listOf(PolicyResult(UUID.randomUUID(), Decision.Permit))

            val result =
                authorizationService.verifiserTilgangTilBruker(
                    TestData.nyNavAnsatt(),
                    listOf(TestData.identitetsnummer1)
                )

            result shouldBe true
            verify { poaoTilgangHttpConsumerMock.evaluatePolicies(any<List<PolicyRequest>>()) }

        }

        "verifiserTilgangTilBruker should return false if access is denied" {
            every {
                poaoTilgangHttpConsumerMock.evaluatePolicies(any<List<PolicyRequest>>())
            } returns listOf(PolicyResult(UUID.randomUUID(), Decision.Deny("test", "test")))

            val result =
                authorizationService.verifiserTilgangTilBruker(
                    TestData.nyNavAnsatt(),
                    listOf(TestData.identitetsnummer1)
                )

            result shouldBe false
            verify { poaoTilgangHttpConsumerMock.evaluatePolicies(any<List<PolicyRequest>>()) }
        }
    }
})
