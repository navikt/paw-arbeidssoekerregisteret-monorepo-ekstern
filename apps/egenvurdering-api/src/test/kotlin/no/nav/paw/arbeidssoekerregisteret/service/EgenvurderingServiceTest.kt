package no.nav.paw.arbeidssoekerregisteret.service

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.paw.arbeidssoekerregisteret.texas.OnBehalfOfResponse
import no.nav.paw.arbeidssoekerregisteret.texas.TexasClient
import no.nav.paw.client.api.oppslag.client.ApiOppslagClient
import no.nav.paw.model.Identitetsnummer

class EgenvurderingServiceTest : FreeSpec({
    val userToken = "userToken"
    val texasClientMock = mockk<TexasClient>().also { texasClient ->
        coEvery { texasClient.getOnBehalfOfToken(userToken) } returns OnBehalfOfResponse(accessToken = "vekslet_token")
    }
    val oppslagsClientMock = mockk<ApiOppslagClient>().also { oppslagClient ->
        coEvery { oppslagClient.findSisteArbeidssoekerperioderAggregert(any()) } returns emptyList()
    }
    val egenvurderingService = EgenvurderingService(
        applicationConfig = mockk(),
        kafkaKeysClient = mockk(),
        producer = mockk(),
        texasClient = texasClientMock,
        oppslagsClient = oppslagsClientMock
    )

    "Henting av egenvurderingsgrunnlag" - {
        "Tomt grunnlag - Egenvurdering og profilering eksisterer ikke for bruker" - {
            val egenvurderingGrunnlag = egenvurderingService.getEgenvurderingGrunnlag(
                userToken = userToken
            )
            egenvurderingGrunnlag shouldNotBe null
            egenvurderingGrunnlag.grunnlag shouldBe null
        }
    }

    "Opprett egenvurdering" - {
        "Produser egenvurdering til Kafka" {}
        "Feilende produsering av egenvurdering til Kafka" {}
    }

})