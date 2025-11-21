package no.nav.paw.oppslagapi.data.consumer.converters

import no.nav.paw.arbeidssokerregisteret.api.v1.AvviksType
import no.nav.paw.arbeidssokerregisteret.api.v1.Beskrivelse
import no.nav.paw.arbeidssokerregisteret.api.v1.BeskrivelseMedDetaljer
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Helse
import no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke
import no.nav.paw.arbeidssokerregisteret.api.v1.Jobbsituasjon
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import no.nav.paw.arbeidssokerregisteret.api.v2.Annet
import no.nav.paw.arbeidssokerregisteret.api.v3.Egenvurdering
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata as MainAvroMetadata
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode as AvroPeriode
import no.nav.paw.arbeidssokerregisteret.api.v1.TidspunktFraKilde as AvroTidspunktFraKilde
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker as AvroOpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.api.v4.Utdanning as AvroUtdanning
import no.nav.paw.oppslagapi.model.v2.Annet as OpenApiAnnet
import no.nav.paw.oppslagapi.model.v2.AvviksType as OpenApiAvviksType
import no.nav.paw.oppslagapi.model.v2.Bruker as OpenApiBruker
import no.nav.paw.oppslagapi.model.v2.BrukerType as OpenApiBrukerType
import no.nav.paw.oppslagapi.model.v2.Helse as OpenApiHelse
import no.nav.paw.oppslagapi.model.v2.Jobbsituasjon as OpenApiJobbsituasjon
import no.nav.paw.oppslagapi.model.v2.Metadata as OpenApiMetadata
import no.nav.paw.oppslagapi.model.v2.OpplysningerOmArbeidssoeker as OpenApiOpplysningerOmArbeidssoeker
import no.nav.paw.oppslagapi.model.v2.Periode as OpenApiPeriode
import no.nav.paw.oppslagapi.model.v2.TidspunktFraKilde as OpenApiTidspunktFraKilde
import no.nav.paw.oppslagapi.model.v2.Utdanning as OpenApiUtdanning

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

fun Bruker.toOpenApi(): OpenApiBruker =
    OpenApiBruker(
        type = when (this.type) {
            BrukerType.VEILEDER -> OpenApiBrukerType.VEILEDER
            BrukerType.SYSTEM -> OpenApiBrukerType.SYSTEM
            BrukerType.SLUTTBRUKER -> OpenApiBrukerType.SLUTTBRUKER
            BrukerType.UKJENT_VERDI -> OpenApiBrukerType.UKJENT_VERDI
            BrukerType.UDEFINERT -> OpenApiBrukerType.UDEFINERT
        },
        id = this.id,
        sikkerhetsnivaa = this.sikkerhetsnivaa
    )

fun AvroTidspunktFraKilde.toOpenApi(): OpenApiTidspunktFraKilde =
    OpenApiTidspunktFraKilde(
        tidspunkt = this.tidspunkt,
        avviksType = when (this.avviksType) {
            AvviksType.UKJENT_VERDI -> OpenApiAvviksType.UKJENT_VERDI
            AvviksType.TIDSPUNKT_KORRIGERT -> OpenApiAvviksType.TIDSPUNKT_KORRIGERT
            AvviksType.FORSINKELSE -> OpenApiAvviksType.FORSINKELSE
            AvviksType.SLETTET -> OpenApiAvviksType.SLETTET
            AvviksType.RETTING -> OpenApiAvviksType.RETTING
        }
    )

fun AvroOpplysningerOmArbeidssoeker.toOpenApi(): OpenApiOpplysningerOmArbeidssoeker =
    OpenApiOpplysningerOmArbeidssoeker(
        sendtInnAv = this.sendtInnAv.toOpenApi(),
        utdanning = this.utdanning?.toOpenApi(),
        helse = this.helse?.toOpenApi(),
        jobbsituasjon = this.jobbsituasjon?.toOpenApi(),
        annet = this.annet?.toOpenApi(),
        id = this.id,
        periodeId = this.periodeId
    )

fun AvroUtdanning.toOpenApi(): OpenApiUtdanning =
    OpenApiUtdanning(
        nus = this.nus,
        bestaatt = this.bestaatt?.toOpenApi(),
        godkjent = this.godkjent?.toOpenApi()
    )

fun JaNeiVetIkke.toOpenApi(): no.nav.paw.oppslagapi.model.v2.JaNeiVetIkke =
    when (this) {
        JaNeiVetIkke.JA -> no.nav.paw.oppslagapi.model.v2.JaNeiVetIkke.JA
        JaNeiVetIkke.NEI -> no.nav.paw.oppslagapi.model.v2.JaNeiVetIkke.NEI
        JaNeiVetIkke.VET_IKKE -> no.nav.paw.oppslagapi.model.v2.JaNeiVetIkke.VET_IKKE
    }

fun Helse.toOpenApi(): OpenApiHelse =
    OpenApiHelse(
        helsetilstandHindrerArbeid = this.helsetilstandHindrerArbeid.toOpenApi()
    )

fun Jobbsituasjon.toOpenApi(): OpenApiJobbsituasjon =
    OpenApiJobbsituasjon(
        beskrivelser = this.beskrivelser.map { it.toOpenApi() }
    )

fun BeskrivelseMedDetaljer.toOpenApi(): no.nav.paw.oppslagapi.model.v2.BeskrivelseMedDetaljer =
    no.nav.paw.oppslagapi.model.v2.BeskrivelseMedDetaljer(
        beskrivelse = this.beskrivelse.toOpenApi(),
        detaljer = this.detaljer
    )

fun Beskrivelse.toOpenApi(): no.nav.paw.oppslagapi.model.v2.Beskrivelse {
    return when (this) {
        Beskrivelse.UKJENT_VERDI -> no.nav.paw.oppslagapi.model.v2.Beskrivelse.UKJENT_VERDI
        Beskrivelse.UDEFINERT -> no.nav.paw.oppslagapi.model.v2.Beskrivelse.UDEFINERT
        Beskrivelse.HAR_SAGT_OPP -> no.nav.paw.oppslagapi.model.v2.Beskrivelse.HAR_SAGT_OPP
        Beskrivelse.HAR_BLITT_SAGT_OPP -> no.nav.paw.oppslagapi.model.v2.Beskrivelse.HAR_BLITT_SAGT_OPP
        Beskrivelse.ER_PERMITTERT -> no.nav.paw.oppslagapi.model.v2.Beskrivelse.ER_PERMITTERT
        Beskrivelse.ALDRI_HATT_JOBB -> no.nav.paw.oppslagapi.model.v2.Beskrivelse.ALDRI_HATT_JOBB
        Beskrivelse.IKKE_VAERT_I_JOBB_SISTE_2_AAR -> no.nav.paw.oppslagapi.model.v2.Beskrivelse.IKKE_VAERT_I_JOBB_SISTE_2_AAR
        Beskrivelse.AKKURAT_FULLFORT_UTDANNING -> no.nav.paw.oppslagapi.model.v2.Beskrivelse.AKKURAT_FULLFORT_UTDANNING
        Beskrivelse.VIL_BYTTE_JOBB -> no.nav.paw.oppslagapi.model.v2.Beskrivelse.VIL_BYTTE_JOBB
        Beskrivelse.USIKKER_JOBBSITUASJON -> no.nav.paw.oppslagapi.model.v2.Beskrivelse.USIKKER_JOBBSITUASJON
        Beskrivelse.MIDLERTIDIG_JOBB -> no.nav.paw.oppslagapi.model.v2.Beskrivelse.MIDLERTIDIG_JOBB
        Beskrivelse.DELTIDSJOBB_VIL_MER -> no.nav.paw.oppslagapi.model.v2.Beskrivelse.DELTIDSJOBB_VIL_MER
        Beskrivelse.NY_JOBB -> no.nav.paw.oppslagapi.model.v2.Beskrivelse.NY_JOBB
        Beskrivelse.KONKURS -> no.nav.paw.oppslagapi.model.v2.Beskrivelse.KONKURS
        Beskrivelse.ANNET -> no.nav.paw.oppslagapi.model.v2.Beskrivelse.ANNET
    }
}


fun Annet.toOpenApi(): OpenApiAnnet =
    OpenApiAnnet(
        andreForholdHindrerArbeid = this.andreForholdHindrerArbeid.toOpenApi()
    )

fun Profilering.toOpenApi(): no.nav.paw.oppslagapi.model.v2.Profilering =
    no.nav.paw.oppslagapi.model.v2.Profilering(
        id = this.id,
        periodeId = this.periodeId,
        opplysningerOmArbeidssokerId = this.opplysningerOmArbeidssokerId,
        jobbetSammenhengendeSeksAvTolvSisteMnd = this.jobbetSammenhengendeSeksAvTolvSisteMnd,
        sendtInnAv = this.sendtInnAv.toOpenApi(),
        alder = this.alder,
        profilertTil = when (this.profilertTil) {
            ProfilertTil.UKJENT_VERDI -> no.nav.paw.oppslagapi.model.v2.ProfilertTil.UKJENT_VERDI
            ProfilertTil.UDEFINERT -> no.nav.paw.oppslagapi.model.v2.ProfilertTil.UDEFINERT
            ProfilertTil.ANTATT_GODE_MULIGHETER -> no.nav.paw.oppslagapi.model.v2.ProfilertTil.ANTATT_GODE_MULIGHETER
            ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING -> no.nav.paw.oppslagapi.model.v2.ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
            ProfilertTil.OPPGITT_HINDRINGER -> no.nav.paw.oppslagapi.model.v2.ProfilertTil.OPPGITT_HINDRINGER
        }
    )

fun Egenvurdering.toOpenApi() = no.nav.paw.oppslagapi.model.v2.Egenvurdering(
    id = this.id,
    periodeId = this.periodeId,
    profileringId = this.profileringId,
    sendtInnAv = this.sendtInnAv.toOpenApi(),
    egenvurdering = when (this.egenvurdering) {
        ProfilertTil.UKJENT_VERDI -> no.nav.paw.oppslagapi.model.v2.ProfilertTil.UKJENT_VERDI
        ProfilertTil.UDEFINERT -> no.nav.paw.oppslagapi.model.v2.ProfilertTil.UDEFINERT
        ProfilertTil.ANTATT_GODE_MULIGHETER -> no.nav.paw.oppslagapi.model.v2.ProfilertTil.ANTATT_GODE_MULIGHETER
        ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING -> no.nav.paw.oppslagapi.model.v2.ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
        ProfilertTil.OPPGITT_HINDRINGER -> no.nav.paw.oppslagapi.model.v2.ProfilertTil.OPPGITT_HINDRINGER
    },
    profilertTil = when (this.profilertTil) {
        ProfilertTil.UKJENT_VERDI -> no.nav.paw.oppslagapi.model.v2.ProfilertTil.UKJENT_VERDI
        ProfilertTil.UDEFINERT -> no.nav.paw.oppslagapi.model.v2.ProfilertTil.UDEFINERT
        ProfilertTil.ANTATT_GODE_MULIGHETER -> no.nav.paw.oppslagapi.model.v2.ProfilertTil.ANTATT_GODE_MULIGHETER
        ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING -> no.nav.paw.oppslagapi.model.v2.ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
        ProfilertTil.OPPGITT_HINDRINGER -> no.nav.paw.oppslagapi.model.v2.ProfilertTil.OPPGITT_HINDRINGER
    }
)
