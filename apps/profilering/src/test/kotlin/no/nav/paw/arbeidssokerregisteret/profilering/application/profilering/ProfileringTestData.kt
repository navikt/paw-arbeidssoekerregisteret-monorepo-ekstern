package no.nav.paw.arbeidssokerregisteret.profilering.application.profilering

import no.nav.paw.aareg.model.*
import no.nav.paw.arbeidssokerregisteret.api.v1.BeskrivelseMedDetaljer
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Helse
import no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke
import no.nav.paw.arbeidssokerregisteret.api.v1.Jobbsituasjon
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import no.nav.paw.arbeidssokerregisteret.api.v2.Annet
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.api.v4.Utdanning
import no.nav.paw.arbeidssokerregisteret.profilering.personinfo.PersonInfo
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.ZoneOffset
import java.util.*

object ProfileringTestData {
    private val today = LocalDate.now()
    const val organisasjonsNummer = "123456789"
    const val identitetsnummer = "12345678911"
    private val uuid = UUID.randomUUID()

    fun standardPeriode(
        periodeId: UUID = uuid,
        startet: Instant? = null
    ) = Periode(
        periodeId,
        identitetsnummer,
        startet?.let(::metadata) ?: metadata,
        null
    )

    fun standardBrukerPersonInfo(
        foedselsdato: LocalDate = LocalDate.of(1986, Month.MAY, 1),
        foedselsAar: Int = 1986,
        arbeidsforhold: List<Arbeidsforhold> = ansattSisteAar
    ): PersonInfo =
        PersonInfo(
            foedselsdato = foedselsdato,
            foedselsAar = foedselsAar,
            arbeidsforhold = arbeidsforhold
        )

    fun standardOpplysninger(
        periodeId: UUID = uuid,
        sendtInnTidspunkt: Instant = today.toInstant(),
        utdanning: Utdanning = utdanning(),
        helse: Helse = helse(),
        jobbsituasjon: Jobbsituasjon = jobbsituasjon(),
        annet: Annet = annet(),
        kilde: String = "junit"
    ): OpplysningerOmArbeidssoeker =
        OpplysningerOmArbeidssoeker(
            UUID.randomUUID(),
            periodeId,
            Metadata(
                sendtInnTidspunkt,
                bruker,
                kilde,
                "unit-test",
                null
            ),
            utdanning,
            helse,
            jobbsituasjon,
            annet
        )

    fun standardProfilering(
        id: UUID = UUID.randomUUID(),
        periodeId: UUID = uuid,
        opplysningerOmArbeidssokerId: UUID = standardOpplysninger().id
    ) = Profilering(
        id,
        periodeId,
        opplysningerOmArbeidssokerId,
        Metadata(
            Instant.now(),
            bruker,
            "test",
            "test",
            null
        ),
        ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING,
        false,
        20
    )

    fun ansettelsesperiode(
        fom: LocalDate = today.minusYears(1),
        tom: LocalDate? = null
    ) = Ansettelsesperiode(
        startdato = fom,
        sluttdato = tom
    )

    fun arbeidsforhold(
        ansettelsesperiode: Ansettelsesperiode = ansettelsesperiode()
    ) = Arbeidsforhold(
        Arbeidssted(
            type = "Arbeidsgiver",
            identer = listOf(Ident("ORGANISASJONSNUMMER", organisasjonsNummer))
        ),
        ansettelsesperiode = ansettelsesperiode,
        opplysningspliktig = Opplysningspliktig(
            type = "",
            identer = listOf(Ident("ORGANISASJONSNUMMER", organisasjonsNummer))
        ),
        ansettelsesdetaljer = listOf(
            Ansettelsesdetaljer(
                type = "Ordinaer",
                avtaltStillingsprosent = 100.0
            )
        ),
        opprettet = today.minusDays(1).atStartOfDay()
    )

    val ansattSisteAar = listOf(arbeidsforhold())

    val bruker = Bruker(BrukerType.SYSTEM, identitetsnummer, null)

    fun metadata(tidspunkt: Instant) = Metadata(
        tidspunkt,
        bruker,
        "test",
        "test",
        null
    )
    val metadata = Metadata(
        today.minusYears(1).toInstant(),
        bruker,
        "test",
        "test",
        null
    )

    val periode = Periode(
        uuid,
        identitetsnummer,
        metadata,
        null
    )

    fun utdanning(
        nus: String = "3",
        bestaatt: JaNeiVetIkke = JaNeiVetIkke.JA,
        godkjent: JaNeiVetIkke = JaNeiVetIkke.JA
    ): Utdanning =
        Utdanning(nus, bestaatt, godkjent)

    fun helse(helsetilstandHindrerArbeid: JaNeiVetIkke = JaNeiVetIkke.NEI): Helse =
        Helse(helsetilstandHindrerArbeid)

    fun jobbsituasjon(beskrivelser: List<BeskrivelseMedDetaljer> = emptyList()): Jobbsituasjon =
        Jobbsituasjon(beskrivelser)

    fun annet(andreForholdHindrerArbeid: JaNeiVetIkke = JaNeiVetIkke.NEI): Annet = Annet(andreForholdHindrerArbeid)

    fun LocalDate.toInstant() = atStartOfDay().toInstant(ZoneOffset.UTC)
}