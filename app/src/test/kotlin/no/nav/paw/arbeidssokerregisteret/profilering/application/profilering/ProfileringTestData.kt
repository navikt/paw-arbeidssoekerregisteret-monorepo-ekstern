package no.nav.paw.arbeidssokerregisteret.profilering.application.profilering

import no.nav.paw.aareg.Ansettelsesperiode
import no.nav.paw.aareg.Arbeidsforhold
import no.nav.paw.aareg.Arbeidsgiver
import no.nav.paw.aareg.Opplysningspliktig
import no.nav.paw.arbeidssokerregisteret.api.v1.Annet
import no.nav.paw.arbeidssokerregisteret.api.v1.Arbeidserfaring
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Helse
import no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke
import no.nav.paw.arbeidssokerregisteret.api.v1.Jobbsituasjon
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import no.nav.paw.arbeidssokerregisteret.api.v3.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.api.v3.Utdanning
import no.nav.paw.arbeidssokerregisteret.profilering.personinfo.PersonInfo
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.ZoneOffset
import java.util.*
import no.nav.paw.aareg.Periode as AaregPeriode

object ProfileringTestData {
    private val today = LocalDate.now()
    const val organisasjonsNummer = "123456789"
    const val identitetsnummer = "12345678911"
    private val uuid = UUID.randomUUID()

    val arbeidsforhold = Arbeidsforhold(
        arbeidsgiver = Arbeidsgiver(
            type = "Arbeidsgiver",
            organisasjonsnummer = organisasjonsNummer
        ),
        ansettelsesperiode = Ansettelsesperiode(
            periode = AaregPeriode(
                fom = today.minusYears(1),
                tom = null
            )
        ),
        opplysningspliktig = Opplysningspliktig(
            type = "",
            organisasjonsnummer = organisasjonsNummer
        ),
        arbeidsavtaler = emptyList(),
        registrert = today.minusDays(1).atStartOfDay()
    )

    val ansattSisteAar = listOf(arbeidsforhold)

    val standardBrukerPersonInfo = PersonInfo(
        foedselsdato = LocalDate.of(1986, Month.MAY, 1),
        foedselsAar = 1986,
        arbeidsforhold = ansattSisteAar
    )

    val bruker = Bruker(BrukerType.SYSTEM, identitetsnummer)

    val standardOpplysningerOmArbeidssoeker = OpplysningerOmArbeidssoeker(
        /* id = */ UUID.randomUUID(),
        /* periodeId = */ uuid,
        /* sendtInnAv = */ Metadata(
            today.toInstant(),
            bruker,
            "junit",
            "unit-test"
        ),
        /* utdanning = */ Utdanning(
            "3",
            JaNeiVetIkke.JA,
            JaNeiVetIkke.JA,
        ),
        /* helse = */ Helse(JaNeiVetIkke.NEI),
        /* arbeidserfaring = */ Arbeidserfaring(JaNeiVetIkke.JA),
        /* jobbsituasjon = */ Jobbsituasjon(emptyList()),
        /* annet = */ Annet(JaNeiVetIkke.NEI)
    )

    val metadata = Metadata(
        today.minusYears(1).toInstant(),
        bruker,
        "test",
        "test"
    )

    val periode = Periode(
        uuid,
        identitetsnummer,
        metadata,
        null
    )

    val avsluttetPeriode = Periode(
        uuid,
        identitetsnummer,
        metadata,
        Metadata(
            today.minusDays(10).toInstant(),
            bruker,
            "test",
            "test"
        ),
    )

    val profilering = Profilering(
        UUID.randomUUID(),
        uuid,
        standardOpplysningerOmArbeidssoeker.id,
        Metadata(
            Instant.now(),
            bruker,
            "test",
            "test"
        ),
        ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING,
        false,
        20
    )

    val personInfo = PersonInfo(LocalDate.of(1986, 11, 26), 1990, listOf(arbeidsforhold))

    fun standardOpplysninger(sendtInnTidspunkt: Instant = today.toInstant()): OpplysningerOmArbeidssoeker =
        OpplysningerOmArbeidssoeker(
            /* id = */ UUID.randomUUID(),
            /* periodeId = */ uuid,
            /* sendtInnAv = */ Metadata(
                sendtInnTidspunkt,
                bruker,
                "junit",
                "unit-test"
            ),
            /* utdanning = */ Utdanning(
                "3",
                JaNeiVetIkke.JA,
                JaNeiVetIkke.JA,
            ),
            /* helse = */ Helse(JaNeiVetIkke.NEI),
            /* arbeidserfaring = */ Arbeidserfaring(JaNeiVetIkke.JA),
            /* jobbsituasjon = */ Jobbsituasjon(emptyList()),
            /* annet = */ Annet(JaNeiVetIkke.NEI)
        )

    fun standardOpplysningerOmArbeidssoekerBuilder(): OpplysningerOmArbeidssoeker.Builder =
        OpplysningerOmArbeidssoeker.newBuilder(standardOpplysningerOmArbeidssoeker)

    fun periodeBuilder(): Periode.Builder = Periode.newBuilder(periode)

    fun metadataBuilder(): Metadata.Builder = Metadata.newBuilder(metadata)

    fun LocalDate.toInstant() = atStartOfDay().toInstant(ZoneOffset.UTC)
}