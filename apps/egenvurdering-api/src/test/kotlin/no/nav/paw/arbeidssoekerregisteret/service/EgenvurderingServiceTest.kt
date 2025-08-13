package no.nav.paw.arbeidssoekerregisteret.service

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.paw.client.api.oppslag.client.ApiOppslagClient
import no.nav.paw.client.api.oppslag.models.AnnetResponse
import no.nav.paw.client.api.oppslag.models.ArbeidssoekerperiodeAggregertResponse
import no.nav.paw.client.api.oppslag.models.BekreftelseResponse
import no.nav.paw.client.api.oppslag.models.BeskrivelseMedDetaljerResponse
import no.nav.paw.client.api.oppslag.models.BrukerResponse
import no.nav.paw.client.api.oppslag.models.BrukerType
import no.nav.paw.client.api.oppslag.models.EgenvurderingResponse
import no.nav.paw.client.api.oppslag.models.HelseResponse
import no.nav.paw.client.api.oppslag.models.JaNeiVetIkke
import no.nav.paw.client.api.oppslag.models.JobbSituasjonBeskrivelse
import no.nav.paw.client.api.oppslag.models.MetadataResponse
import no.nav.paw.client.api.oppslag.models.OpplysningerOmArbeidssoekerAggregertResponse
import no.nav.paw.client.api.oppslag.models.ProfileringAggregertResponse
import no.nav.paw.client.api.oppslag.models.ProfileringsResultat
import no.nav.paw.client.api.oppslag.models.TidspunktFraKildeResponse
import no.nav.paw.client.api.oppslag.models.UtdanningResponse
import no.nav.paw.model.Identitetsnummer
import no.nav.paw.security.authentication.model.Claims
import no.nav.paw.security.authentication.model.TokenX
import no.nav.paw.security.authentication.token.AccessToken
import no.nav.paw.security.texas.OnBehalfOfResponse
import no.nav.paw.security.texas.TexasClient
import java.time.Instant
import java.util.*

class EgenvurderingServiceTest : FreeSpec({
    val accessToken = AccessToken(
        jwt = "dummy-jwt",
        issuer = TokenX,
        claims = Claims(emptyMap()),
    )
    val texasClientMock = mockk<TexasClient>(relaxed = true).also { texasClient ->
        coEvery { texasClient.exchangeOnBehalfOfBrukerToken(any()) } returns OnBehalfOfResponse("vekslet-token")
    }
    val oppslagsClientMock = mockk<ApiOppslagClient>().also { oppslagClient ->
        coEvery { oppslagClient.findSisteArbeidssoekerperioderAggregert(any()) } returns emptyList()
    }
    val egenvurderingService = EgenvurderingService(
        applicationConfig = mockk(relaxed = true),
        kafkaKeysClient = mockk(relaxed = true),
        producer = mockk(relaxed = true),
        texasClient = texasClientMock,
        oppslagsClient = oppslagsClientMock
    )

    "Henting av egenvurderingsgrunnlag" - {
        "Tomt grunnlag - Profilering eksisterer ikke for bruker" - {
            coEvery { oppslagsClientMock.findSisteArbeidssoekerperioderAggregert(any()) } returns listOf(
                TestData.nyArbeidssoekerperiodeAggregertResponse(
                    opplysningerOmArbeidssoeker = TestData.nyOpplysningerOmArbeidssoekerAggregertResponse(
                        profilering = null
                    ),
                )
            )
            val egenvurderingGrunnlag = egenvurderingService.getEgenvurderingGrunnlag(accessToken)
            egenvurderingGrunnlag shouldNotBe null
            egenvurderingGrunnlag.grunnlag shouldBe null
        }

        "Tomt grunnlag - Egenvurdering eksisterer fra før for profilering" - {
            coEvery { oppslagsClientMock.findSisteArbeidssoekerperioderAggregert(any()) } returns listOf(
                TestData.nyArbeidssoekerperiodeAggregertResponse(
                    opplysningerOmArbeidssoeker = TestData.nyOpplysningerOmArbeidssoekerAggregertResponse(
                        profilering = TestData.nyProfileringAggregertResponse(
                            egenvurderinger = listOf(TestData.nyEgenvurderingResponse())
                        )
                    ),
                )
            )
            val egenvurderingGrunnlag = egenvurderingService.getEgenvurderingGrunnlag(
                accessToken = accessToken
            )
            egenvurderingGrunnlag shouldNotBe null
            egenvurderingGrunnlag.grunnlag shouldBe null
        }
        "Tomt grunnlag - Periode er avsluttet" - {
            coEvery { oppslagsClientMock.findSisteArbeidssoekerperioderAggregert(any()) } returns listOf(
                TestData.nyArbeidssoekerperiodeAggregertResponse(
                    avsluttet = TestData.nyMetadataResponse(),
                    opplysningerOmArbeidssoeker = TestData.nyOpplysningerOmArbeidssoekerAggregertResponse(
                        profilering = TestData.nyProfileringAggregertResponse(
                            egenvurderinger = emptyList()
                        )
                    ),
                )
            )
            val egenvurderingGrunnlag = egenvurderingService.getEgenvurderingGrunnlag(
                accessToken = accessToken
            )
            egenvurderingGrunnlag shouldNotBe null
            egenvurderingGrunnlag.grunnlag shouldBe null
        }
        "Gyldig grunnlag - Profilering eksisterer for bruker og egenvurdering er ikke besvart" - {
            coEvery { oppslagsClientMock.findSisteArbeidssoekerperioderAggregert(any()) } returns listOf(
                TestData.nyArbeidssoekerperiodeAggregertResponse(
                    opplysningerOmArbeidssoeker = TestData.nyOpplysningerOmArbeidssoekerAggregertResponse(
                        profilering = TestData.nyProfileringAggregertResponse(
                            egenvurderinger = emptyList()
                        )
                    ),
                )
            )
            val egenvurderingGrunnlag = egenvurderingService.getEgenvurderingGrunnlag(
                accessToken = accessToken
            )
            egenvurderingGrunnlag shouldNotBe null
            egenvurderingGrunnlag.grunnlag shouldNotBe null
        }
    }

    "Opprett egenvurdering" - {
        "Produser egenvurdering til Kafka" {}
        "Feilende produsering av egenvurdering til Kafka" {}
    }

})

object TestData {
    private val defaultIdent = Identitetsnummer("12345678901")
    private val periodeId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
    private val opplysningerOmArbeidssoekerId = UUID.fromString("123e4567-e89b-12d3-a456-426614174001")
    private val profileringId = UUID.fromString("123e4567-e89b-12d3-a456-426614174002")
    private val egenvurderingId = UUID.fromString("123e4567-e89b-12d3-a456-426614174003")

    fun nyArbeidssoekerperiodeAggregertResponse(
        periodeId: UUID = TestData.periodeId,
        startet: MetadataResponse = nyMetadataResponse(),
        avsluttet: MetadataResponse? = null,
        opplysningerOmArbeidssoeker: List<OpplysningerOmArbeidssoekerAggregertResponse> = nyOpplysningerOmArbeidssoekerAggregertResponse(),
        bekreftelser: List<BekreftelseResponse> = emptyList(),
    ): ArbeidssoekerperiodeAggregertResponse =
        ArbeidssoekerperiodeAggregertResponse(
            periodeId = periodeId,
            startet = startet,
            avsluttet = avsluttet,
            opplysningerOmArbeidssoeker = opplysningerOmArbeidssoeker,
            bekreftelser = bekreftelser
        )

    fun nyOpplysningerOmArbeidssoekerAggregertResponse(
        opplysningerOmArbeidssoekerId: UUID = TestData.opplysningerOmArbeidssoekerId,
        periodeId: UUID = TestData.periodeId,
        sendtInnAv: MetadataResponse = nyMetadataResponse(),
        jobbsituasjon: List<BeskrivelseMedDetaljerResponse> = nyJobbsituasjonResponse(),
        utdanning: UtdanningResponse = nyUtdanningResponse(),
        helse: HelseResponse = nyHelseResponse(),
        annet: AnnetResponse = nyAnnetResponse(),
        profilering: ProfileringAggregertResponse? = nyProfileringAggregertResponse(),
    ): List<OpplysningerOmArbeidssoekerAggregertResponse> =
        listOf(
            OpplysningerOmArbeidssoekerAggregertResponse(
                opplysningerOmArbeidssoekerId = opplysningerOmArbeidssoekerId,
                periodeId = periodeId,
                sendtInnAv = sendtInnAv,
                jobbsituasjon = jobbsituasjon,
                utdanning = utdanning,
                helse = helse,
                annet = annet,
                profilering = profilering
            )
        )

    fun nyJobbsituasjonResponse(): List<BeskrivelseMedDetaljerResponse> =
        listOf(
            BeskrivelseMedDetaljerResponse(
                beskrivelse = JobbSituasjonBeskrivelse.HAR_BLITT_SAGT_OPP,
                detaljer = mapOf("dato" to "2023-01-01", "årsak" to "Omorganisering")
            ),
            BeskrivelseMedDetaljerResponse(
                beskrivelse = JobbSituasjonBeskrivelse.DELTIDSJOBB_VIL_MER,
                detaljer = mapOf("arbeidstid" to "20 timer per uke", "ønsket_stilling" to "fulltid")
            )
        )

    fun nyUtdanningResponse(): UtdanningResponse =
        UtdanningResponse(
            nus = "1234",
            bestaatt = JaNeiVetIkke.JA,
            godkjent = JaNeiVetIkke.NEI
        )

    fun nyHelseResponse(): HelseResponse =
        HelseResponse(
            helsetilstandHindrerArbeid = JaNeiVetIkke.NEI
        )

    fun nyAnnetResponse(): AnnetResponse = AnnetResponse(
        andreForholdHindrerArbeid = JaNeiVetIkke.NEI,
    )

    fun nyProfileringAggregertResponse(
        profileringId: UUID = TestData.profileringId,
        periodeId: UUID = TestData.periodeId,
        opplysningerOmArbeidssoekerId: UUID = TestData.opplysningerOmArbeidssoekerId,
        sendtInnAv: MetadataResponse = nyMetadataResponse(),
        profilertTil: ProfileringsResultat = ProfileringsResultat.ANTATT_GODE_MULIGHETER,
        jobbetSammenhengendeSeksAvTolvSisteManeder: Boolean = false,
        alder: Int = 30,
        egenvurderinger: List<EgenvurderingResponse> = listOf(nyEgenvurderingResponse()),
    ): ProfileringAggregertResponse =
        ProfileringAggregertResponse(
            profileringId = profileringId,
            periodeId = periodeId,
            opplysningerOmArbeidssoekerId = opplysningerOmArbeidssoekerId,
            sendtInnAv = sendtInnAv,
            profilertTil = profilertTil,
            jobbetSammenhengendeSeksAvTolvSisteManeder = jobbetSammenhengendeSeksAvTolvSisteManeder,
            alder = alder,
            egenvurderinger = egenvurderinger
        )

    fun nyEgenvurderingResponse(
        egenvurderingId: UUID = TestData.egenvurderingId,
        periodeId: UUID = TestData.periodeId,
        opplysningerOmArbeidssoekerId: UUID = TestData.opplysningerOmArbeidssoekerId,
        profileringId: UUID = TestData.profileringId,
        sendtInnAv: MetadataResponse = nyMetadataResponse(),
        profilertTil: ProfileringsResultat = ProfileringsResultat.ANTATT_GODE_MULIGHETER,
        egenvurdering: ProfileringsResultat = ProfileringsResultat.ANTATT_GODE_MULIGHETER,
    ): EgenvurderingResponse =
        EgenvurderingResponse(
            egenvurderingId = egenvurderingId,
            periodeId = periodeId,
            opplysningerOmArbeidssoekerId = opplysningerOmArbeidssoekerId,
            profileringId = profileringId,
            sendtInnAv = sendtInnAv,
            profilertTil = profilertTil,
            egenvurdering = egenvurdering
        )

    fun nyMetadataResponse(
        tidspunkt: Instant = Instant.now(),
        utfoertAv: BrukerResponse = nyBrukerResponse(),
        kilde: String = "test-kilde",
        aarsak: String = "test-aarsak",
        tidspunktFraKilde: TidspunktFraKildeResponse? = null,
    ): MetadataResponse =
        MetadataResponse(
            tidspunkt = tidspunkt,
            utfoertAv = utfoertAv,
            kilde = kilde,
            aarsak = aarsak,
            tidspunktFraKilde = tidspunktFraKilde
        )

    fun nyBrukerResponse(
        type: BrukerType = BrukerType.SLUTTBRUKER,
        id: String = defaultIdent.verdi,
    ): BrukerResponse =
        BrukerResponse(
            type = type,
            id = id
        )
}