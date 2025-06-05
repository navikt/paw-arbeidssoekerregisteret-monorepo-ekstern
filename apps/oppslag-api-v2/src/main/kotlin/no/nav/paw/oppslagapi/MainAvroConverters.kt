package no.nav.paw.oppslagapi

import no.nav.paw.arbeidssokerregisteret.api.v1.Beskrivelse
import no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode as AvroPeriode
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata as MainAvroMetadata
import no.nav.paw.arbeidssokerregisteret.api.v1.TidspunktFraKilde as AvroTidspunktFraKilde
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Periode as OpenApiPeriode
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Metadata as OpenApiMetadata
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Bruker as OpenApiBruker
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.TidspunktFraKilde as OpenApiTidspunktFraKilde
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.AvviksType as OpenApiAvviksType
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker as AvroOpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.api.v4.Utdanning as AvroUtdanning
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.OpplysningerOmArbeidssoeker as OpenApiOpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Utdanning as OpenApiUtdanning
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Helse as OpenApiHelse
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Jobbsituasjon as OpenApiJobbsituasjon
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Annet as OpenApiAnnet

fun AvroPeriode.toOpenApi(): OpenApiPeriode =
    OpenApiPeriode(
        id = this.id,
        identitetsnummer = this.identitetsnummer,
        startet = this.startet.toOpenApi(),
        avsluttet = this.avsluttet?.toOpenApi()
    )

fun MainAvroMetadata.toOpenApi(): OpenApiMetadata =
    OpenApiMetadata(
        tidspunkt = this.tidspunkt,
        utfoertAv = this.utfoertAv.toOpenApi(),
        kilde = this.kilde,
        aarsak = this.aarsak,
        tidspunktFraKilde = this.tidspunktFraKilde?.toOpenApi()
    )

fun no.nav.paw.arbeidssokerregisteret.api.v1.Bruker.toOpenApi(): OpenApiBruker =
    OpenApiBruker(
        type = OpenApiBruker.Type.valueOf(this.type.name),
        id = this.id,
        sikkerhetsnivaa = this.sikkerhetsnivaa
    )

fun AvroTidspunktFraKilde.toOpenApi(): OpenApiTidspunktFraKilde =
    OpenApiTidspunktFraKilde(
        tidspunkt = this.tidspunkt,
        avviksType = OpenApiAvviksType.valueOf(this.avviksType.name)
    )

fun AvroOpplysningerOmArbeidssoeker.toOpenApi(): OpenApiOpplysningerOmArbeidssoeker =
    OpenApiOpplysningerOmArbeidssoeker(
        sendtInnAv = this.sendtInnAv.toOpenApi(),
        utdanning = this.utdanning?.toOpenApi(),
        helse = this.helse?.toOpenApi(),
        jobbsituasjon = this.jobbsituasjon?.toOpenApi(),
        annet = this.annet?.toOpenApi()
    )

fun AvroUtdanning.toOpenApi(): OpenApiUtdanning =
    OpenApiUtdanning(
        nus = this.nus,
        bestaatt = this.bestaatt?.toOpenApi(),
        godkjent = this.godkjent?.toOpenApi()
    )

fun JaNeiVetIkke.toOpenApi(): no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.JaNeiVetIkke =
    when (this) {
        JaNeiVetIkke.JA -> no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.JaNeiVetIkke.JA
        JaNeiVetIkke.NEI -> no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.JaNeiVetIkke.NEI
        JaNeiVetIkke.VET_IKKE -> no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.JaNeiVetIkke.VET_IKKE
    }

fun no.nav.paw.arbeidssokerregisteret.api.v1.Helse.toOpenApi(): OpenApiHelse =
    OpenApiHelse(
        helsetilstandHindrerArbeid = this.helsetilstandHindrerArbeid.toOpenApi()
    )

fun no.nav.paw.arbeidssokerregisteret.api.v1.Jobbsituasjon.toOpenApi(): OpenApiJobbsituasjon =
    OpenApiJobbsituasjon(
        beskrivelser = this.beskrivelser.map { it.toOpenApi() }
    )

fun no.nav.paw.arbeidssokerregisteret.api.v1.BeskrivelseMedDetaljer.toOpenApi(): no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.BeskrivelseMedDetaljer =
    no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.BeskrivelseMedDetaljer(
        beskrivelse = this.beskrivelse.toOpenApi(),
        detaljer = this.detaljer
    )

fun Beskrivelse.toOpenApi(): no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Beskrivelse {
    return when (this) {
        Beskrivelse.UKJENT_VERDI -> no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Beskrivelse.UKJENT_VERDI
        Beskrivelse.UDEFINERT -> no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Beskrivelse.UDEFINERT
        Beskrivelse.HAR_SAGT_OPP -> no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Beskrivelse.HAR_SAGT_OPP
        Beskrivelse.HAR_BLITT_SAGT_OPP -> no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Beskrivelse.HAR_BLITT_SAGT_OPP
        Beskrivelse.ER_PERMITTERT -> no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Beskrivelse.ER_PERMITTERT
        Beskrivelse.ALDRI_HATT_JOBB -> no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Beskrivelse.ALDRI_HATT_JOBB
        Beskrivelse.IKKE_VAERT_I_JOBB_SISTE_2_AAR -> no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Beskrivelse.IKKE_VAERT_I_JOBB_SISTE_2_AAR
        Beskrivelse.AKKURAT_FULLFORT_UTDANNING -> no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Beskrivelse.AKKURAT_FULLFORT_UTDANNING
        Beskrivelse.VIL_BYTTE_JOBB -> no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Beskrivelse.VIL_BYTTE_JOBB
        Beskrivelse.USIKKER_JOBBSITUASJON -> no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Beskrivelse.USIKKER_JOBBSITUASJON
        Beskrivelse.MIDLERTIDIG_JOBB -> no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Beskrivelse.MIDLERTIDIG_JOBB
        Beskrivelse.DELTIDSJOBB_VIL_MER -> no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Beskrivelse.DELTIDSJOBB_VIL_MER
        Beskrivelse.NY_JOBB -> no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Beskrivelse.NY_JOBB
        Beskrivelse.KONKURS -> no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Beskrivelse.KONKURS
        Beskrivelse.ANNET -> no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Beskrivelse.ANNET
    }
}



fun no.nav.paw.arbeidssokerregisteret.api.v2.Annet.toOpenApi(): OpenApiAnnet =
    OpenApiAnnet(
        andreForholdHindrerArbeid = this.andreForholdHindrerArbeid.toOpenApi()
    )

fun Profilering.toOpenApi(): no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Profilering =
    no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Profilering(
        id = this.id,
        periodeId = this.periodeId,
        opplysningerOmArbeidssokerId = this.opplysningerOmArbeidssokerId,
        jobbetSammenhengendeSeksAvTolvSisteMnd = this.jobbetSammenhengendeSeksAvTolvSisteMnd,
        sendtInnAv = this.sendtInnAv.toOpenApi(),
        alder = this.alder,
        profilertTil = when (this.profilertTil) {
            ProfilertTil.UKJENT_VERDI -> no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.ProfilertTil.UKJENT_VERDI
            ProfilertTil.UDEFINERT -> no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.ProfilertTil.UDEFINERT
            ProfilertTil.ANTATT_GODE_MULIGHETER -> no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.ProfilertTil.ANTATT_GODE_MULIGHETER
            ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING -> no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
            ProfilertTil.OPPGITT_HINDRINGER -> no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.ProfilertTil.OPPGITT_HINDRINGER
        }
    )