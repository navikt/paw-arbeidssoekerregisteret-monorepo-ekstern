package no.nav.paw.arbeidssoekerregisteret.service

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.paw.arbeidssoekerregisteret.clients.OnBehalfOfResponse
import no.nav.paw.arbeidssoekerregisteret.clients.TexasClient
import no.nav.paw.client.api.oppslag.client.ApiOppslagClient
import no.nav.paw.model.Identitetsnummer

class EgenvurderingServiceTest : FreeSpec({
    val identitetsnummer = Identitetsnummer(verdi = "12345123451")
    val userToken = "userToken"
    val texasClientMock = mockk<TexasClient>().also { texasClient ->
        coEvery { texasClient.getOnBehalfOfToken(userToken) } returns OnBehalfOfResponse(accessToken = "vekslet_token")
    }
    val oppslagsClientMock = mockk<ApiOppslagClient>().also { oppslagClient ->
        coEvery { oppslagClient.findEgenvurdering(any()) } returns emptyList()
        coEvery { oppslagClient.findProfilering(any()) } returns emptyList()
    }
    val egenvurderingService = EgenvurderingService(
        applicationConfig = mockk(),
        kafkaConfig = mockk(),
        kafkaKeysClient = mockk(),
        producer = mockk(),
        texasClient = texasClientMock,
        oppslagsClient = oppslagsClientMock
    )

    "Henting av egenvurderingsgrunnlag" - {
        "Tomt grunnlag - Egenvurdering og profilering eksisterer ikke for bruker" - {
            val egenvurderingGrunnlag = egenvurderingService.getEgenvurderingGrunnlag(identitetsnummer, userToken)
            egenvurderingGrunnlag!!.grunnlag shouldBe null
        }
    }

    "Opprett egenvurdering" - {
        "Produser egenvurdering til Kafka" {}
        "Feilende produsering av egenvurdering til Kafka" {}
    }

})