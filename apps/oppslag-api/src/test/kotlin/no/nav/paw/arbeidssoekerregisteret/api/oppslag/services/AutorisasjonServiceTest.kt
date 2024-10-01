package no.nav.paw.arbeidssoekerregisteret.api.oppslag.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.TestData
import no.nav.poao_tilgang.client.Decision
import no.nav.poao_tilgang.client.PoaoTilgangCachedClient
import no.nav.poao_tilgang.client.api.ApiResult

class AutorisasjonServiceTest : FunSpec({
    test("verifiserTilgangTilBruker should return true if access is granted") {
        val poaoTilgangHttpClient = mockk<PoaoTilgangCachedClient>()
        val autorisasjonService = AutorisasjonService(poaoTilgangHttpClient)

        val navAnsatt = TestData.navAnsatt
        val foedselsnummer = TestData.foedselsnummer

        every { poaoTilgangHttpClient.evaluatePolicy(any()) } returns
            ApiResult(
                throwable = null,
                result = Decision.Permit
            )

        val result = autorisasjonService.verifiserTilgangTilBruker(navAnsatt, foedselsnummer)

        result shouldBe true
    }

    test("verifiserTilgangTilBruker should return false if access is denied") {
        val poaoTilgangHttpClient = mockk<PoaoTilgangCachedClient>()
        val autorisasjonService = AutorisasjonService(poaoTilgangHttpClient)

        val navAnsatt = TestData.navAnsatt
        val foedselsnummer = TestData.foedselsnummer

        every { poaoTilgangHttpClient.evaluatePolicy(any()) } returns
            ApiResult(
                throwable = null,
                result = Decision.Deny("", "")
            )

        val result = autorisasjonService.verifiserTilgangTilBruker(navAnsatt, foedselsnummer)

        result shouldBe false
    }
})
