package no.nav.paw.arbeidssoekerregisteret.api.oppslag.services

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.exception.PeriodeIkkeFunnetException
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.ApplicationTestContext
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.TestData
import no.nav.paw.pdl.graphql.generated.enums.IdentGruppe
import no.nav.paw.pdl.graphql.generated.hentidenter.IdentInformasjon
import no.nav.paw.security.authentication.model.Identitetsnummer
import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.security.authentication.model.resolveBruker
import no.nav.paw.security.authorization.exception.IngenTilgangException
import no.nav.paw.security.authorization.model.Action
import no.nav.poao_tilgang.api.dto.response.DecisionType
import java.util.*

class AuthorizationServiceTest : FreeSpec({
    with(ApplicationTestContext.withMockDataAccess()) {

        afterSpec {
            confirmVerified(
                periodeRepository,
                pdlHttpConsumerMock,
                poaoTilgangHttpConsumerMock
            )
        }

        "Skal finne identer for fnr" {
            val identer: List<IdentInformasjon> = listOf(
                IdentInformasjon(TestData.fnr1, IdentGruppe.FOLKEREGISTERIDENT),
                IdentInformasjon("12345", IdentGruppe.NPID),
                IdentInformasjon("23456", IdentGruppe.NPID),
                IdentInformasjon("98765", IdentGruppe.AKTORID),
                IdentInformasjon(TestData.fnr2, IdentGruppe.FOLKEREGISTERIDENT),
                IdentInformasjon("87654", IdentGruppe.__UNKNOWN_VALUE)
            )
            coEvery {
                pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>())
            } returns identer

            val identiteter = authorizationService.finnIdentiteter(TestData.identitetsnummer1)

            identiteter shouldHaveSize 2
            identiteter shouldContain TestData.identitetsnummer1
            identiteter shouldContain TestData.identitetsnummer2

            val securityContext = authorizationService.utvidPrincipal(
                TestData.nySecurityContext(
                    bruker = TestData.nySluttbruker(identitetsnummer = TestData.identitetsnummer3)
                )
            )

            val bruker = securityContext.resolveBruker<Sluttbruker>()

            bruker.ident shouldBe TestData.identitetsnummer3
            identiteter shouldHaveSize 2
            identiteter shouldContain TestData.identitetsnummer1
            identiteter shouldContain TestData.identitetsnummer2

            coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
        }

        "Sluttbruker-policies skal tilgangstyre" {
            coEvery {
                periodeRepository.hentPeriodeForId(TestData.periodeId1)
            } returns TestData.nyStartetPeriodeRow(identitetsnummer = TestData.fnr4)
            coEvery {
                periodeRepository.hentPeriodeForId(TestData.periodeId2)
            } returns null
            val sluttbrukerSecurityContext = TestData.nySecurityContext(
                bruker = TestData.nySluttbruker(identitetsnummer = TestData.identitetsnummer4)
            )
            val navAnsattSecurityContext = TestData.nySecurityContext(bruker = TestData.nyNavAnsatt())
            val m2mTokenSecurityContext = TestData.nySecurityContext(bruker = TestData.nyM2MToken())
            val policies1 = authorizationService.sluttbrukerAccessPolicies()
            val policies2 = authorizationService.sluttbrukerAccessPolicies(null)
            val policies3 = authorizationService.sluttbrukerAccessPolicies(TestData.periodeId1)
            val policies4 = authorizationService.sluttbrukerAccessPolicies(TestData.periodeId2)

            policies1 shouldHaveSize 1
            policies2 shouldHaveSize 1
            policies3 shouldHaveSize 1
            policies4 shouldHaveSize 1
            val policy1 = policies1[0]
            val policy2 = policies2[0]
            val policy3 = policies3[0]
            val policy4 = policies4[0]

            policy1.checkAccess(Action.READ, sluttbrukerSecurityContext)
            shouldThrow<IngenTilgangException> {
                policy1.checkAccess(Action.READ, navAnsattSecurityContext)
            }
            shouldThrow<IngenTilgangException> {
                policy1.checkAccess(Action.READ, m2mTokenSecurityContext)
            }
            policy2.checkAccess(Action.READ, sluttbrukerSecurityContext)
            shouldThrow<IngenTilgangException> {
                policy2.checkAccess(Action.READ, navAnsattSecurityContext)
            }
            shouldThrow<IngenTilgangException> {
                policy2.checkAccess(Action.READ, m2mTokenSecurityContext)
            }
            policy3.checkAccess(Action.READ, sluttbrukerSecurityContext)
            shouldThrow<IngenTilgangException> {
                policy3.checkAccess(Action.READ, navAnsattSecurityContext)
            }
            shouldThrow<IngenTilgangException> {
                policy3.checkAccess(Action.READ, m2mTokenSecurityContext)
            }
            shouldThrow<PeriodeIkkeFunnetException> {
                policy4.checkAccess(Action.READ, sluttbrukerSecurityContext)
            }
            shouldThrow<IngenTilgangException> {
                policy4.checkAccess(Action.READ, navAnsattSecurityContext)
            }
            shouldThrow<IngenTilgangException> {
                policy4.checkAccess(Action.READ, m2mTokenSecurityContext)
            }

            coVerify { periodeRepository.hentPeriodeForId(any<UUID>()) }
        }

        "Veileder-policies skal tilgangstyre" {
            coEvery {
                periodeRepository.hentPeriodeForId(TestData.periodeId3)
            } returns TestData.nyStartetPeriodeRow(identitetsnummer = TestData.fnr5)
            coEvery {
                periodeRepository.hentPeriodeForId(TestData.periodeId4)
            } returns null
            coEvery {
                pdlHttpConsumerMock.finnIdenter(TestData.identitetsnummer5)
            } returns listOf(IdentInformasjon(TestData.fnr5, IdentGruppe.FOLKEREGISTERIDENT))
            coEvery {
                poaoTilgangHttpConsumerMock.evaluatePolicies(any(), any(), any())
            } returns TestData.nyEvaluatePoliciesResponse(DecisionType.PERMIT, DecisionType.PERMIT)
            val sluttbrukerSecurityContext = TestData.nySecurityContext(bruker = TestData.nySluttbruker())
            val navAnsattSecurityContext = TestData.nySecurityContext(bruker = TestData.nyNavAnsatt())
            val m2mTokenSecurityContext = TestData.nySecurityContext(bruker = TestData.nyM2MToken())
            val policies1 = authorizationService.veilederAccessPolicies(TestData.periodeId3)
            shouldThrow<PeriodeIkkeFunnetException> {
                authorizationService.veilederAccessPolicies(TestData.periodeId4)
            }
            val policies2 = authorizationService.veilederAccessPolicies(listOf(TestData.identitetsnummer5))
            val policies3 = authorizationService.veilederAccessPolicies(emptyList())
            val policies4 = authorizationService
                .veilederAccessPolicies(TestData.periodeId3, listOf(TestData.identitetsnummer5))
            val policies5 = authorizationService
                .veilederAccessPolicies(TestData.periodeId3, emptyList())
            val policies6 = authorizationService
                .veilederAccessPolicies(TestData.periodeId4, listOf(TestData.identitetsnummer5))

            policies1 shouldHaveSize 1
            policies2 shouldHaveSize 1
            policies3 shouldHaveSize 1
            policies4 shouldHaveSize 1
            policies5 shouldHaveSize 1
            policies6 shouldHaveSize 1
            val policy1 = policies1[0]
            val policy2 = policies2[0]
            val policy3 = policies3[0]
            val policy4 = policies4[0]
            val policy5 = policies5[0]
            val policy6 = policies6[0]

            policy1.checkAccess(Action.READ, navAnsattSecurityContext)
            policy1.checkAccess(Action.READ, m2mTokenSecurityContext)
            shouldThrow<IngenTilgangException> {
                policy1.checkAccess(Action.READ, sluttbrukerSecurityContext)
            }
            policy2.checkAccess(Action.READ, navAnsattSecurityContext)
            policy2.checkAccess(Action.READ, m2mTokenSecurityContext)
            shouldThrow<IngenTilgangException> {
                policy2.checkAccess(Action.READ, sluttbrukerSecurityContext)
            }
            shouldThrow<IngenTilgangException> {
                policy3.checkAccess(Action.READ, navAnsattSecurityContext)
            }
            shouldThrow<IngenTilgangException> {
                policy3.checkAccess(Action.READ, m2mTokenSecurityContext)
            }
            shouldThrow<IngenTilgangException> {
                policy3.checkAccess(Action.READ, sluttbrukerSecurityContext)
            }
            policy4.checkAccess(Action.READ, navAnsattSecurityContext)
            policy4.checkAccess(Action.READ, m2mTokenSecurityContext)
            shouldThrow<IngenTilgangException> {
                policy4.checkAccess(Action.READ, sluttbrukerSecurityContext)
            }
            shouldThrow<IngenTilgangException> {
                policy5.checkAccess(Action.READ, navAnsattSecurityContext)
            }
            shouldThrow<IngenTilgangException> {
                policy5.checkAccess(Action.READ, m2mTokenSecurityContext)
            }
            shouldThrow<IngenTilgangException> {
                policy5.checkAccess(Action.READ, sluttbrukerSecurityContext)
            }
            shouldThrow<PeriodeIkkeFunnetException> {
                policy6.checkAccess(Action.READ, navAnsattSecurityContext)
            }
            shouldThrow<PeriodeIkkeFunnetException> {
                policy6.checkAccess(Action.READ, m2mTokenSecurityContext)
            }
            shouldThrow<IngenTilgangException> {
                policy6.checkAccess(Action.READ, sluttbrukerSecurityContext)
            }

            coVerify { periodeRepository.hentPeriodeForId(any<UUID>()) }
            coVerify { pdlHttpConsumerMock.finnIdenter(any<Identitetsnummer>()) }
            coVerify { poaoTilgangHttpConsumerMock.evaluatePolicies(any(), any(), any()) }
        }
    }
})
