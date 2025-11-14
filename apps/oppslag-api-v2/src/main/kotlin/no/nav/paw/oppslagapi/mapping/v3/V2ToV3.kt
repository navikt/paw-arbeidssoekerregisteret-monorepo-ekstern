package no.nav.paw.oppslagapi.mapping.v3

import no.nav.paw.oppslagapi.model.v3.Annet
import no.nav.paw.oppslagapi.model.v3.AvviksType
import no.nav.paw.oppslagapi.model.v3.BekreftelsStatus
import no.nav.paw.oppslagapi.model.v3.Bekreftelse
import no.nav.paw.oppslagapi.model.v3.BekreftelseMedMetadata
import no.nav.paw.oppslagapi.model.v3.Bekreftelsesloesning
import no.nav.paw.oppslagapi.model.v3.Beskrivelse
import no.nav.paw.oppslagapi.model.v3.BeskrivelseMedDetaljer
import no.nav.paw.oppslagapi.model.v3.Bruker
import no.nav.paw.oppslagapi.model.v3.BrukerType
import no.nav.paw.oppslagapi.model.v3.Egenvurdering
import no.nav.paw.oppslagapi.model.v3.Helse
import no.nav.paw.oppslagapi.model.v3.Hendelse
import no.nav.paw.oppslagapi.model.v3.HendelseType
import no.nav.paw.oppslagapi.model.v3.JaNeiVetIkke
import no.nav.paw.oppslagapi.model.v3.Jobbsituasjon
import no.nav.paw.oppslagapi.model.v3.Metadata
import no.nav.paw.oppslagapi.model.v3.OpplysningerOmArbeidssoeker
import no.nav.paw.oppslagapi.model.v3.PaaVegneAvStart
import no.nav.paw.oppslagapi.model.v3.PaaVegneAvStopp
import no.nav.paw.oppslagapi.model.v3.Profilering
import no.nav.paw.oppslagapi.model.v3.ProfilertTil
import no.nav.paw.oppslagapi.model.v3.Svar
import no.nav.paw.oppslagapi.model.v3.Tidslinje
import no.nav.paw.oppslagapi.model.v3.TidspunktFraKilde
import no.nav.paw.oppslagapi.model.v3.Utdanning
import java.util.UUID

fun no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Tidslinje.asV3(): Tidslinje {
    return Tidslinje(
        periodeId = this.periodeId,
        identitetsnummer = this.identitetsnummer,
        startet = this.startet,
        avsluttet = this.avsluttet,
        hendelser = this.hendelser.map { it.asV3() }
    )
}

fun no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Hendelse.asV3(): Hendelse {
    return when (this.hendelseType) {
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.HendelseType.periode_startet_v1 -> this.asV3PeriodeStartet()
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.HendelseType.periode_avsluttet_v1 -> this.asV3PeriodeAvsluttet()
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.HendelseType.opplysninger_v4 -> this.asV3Opplysninger()
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.HendelseType.profilering_v1 -> this.asV3Profilering()
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.HendelseType.egenvurdering_v1 -> this.asV3Egenvurdering()
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.HendelseType.bekreftelse_v1 -> this.asV3Bekreftelse()
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.HendelseType.pa_vegne_av_start_v1 -> this.asV3PaaVegneAvStart()
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.HendelseType.pa_vegne_av_stopp_v1 -> this.asV3PaaVegneAvStopp()
    }
}

fun no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Hendelse.asV3PeriodeStartet(): Hendelse {
    return this.periodeStartetV1?.asV3(HendelseType.PERIODE_STARTET_V1)
        ?: throw IllegalArgumentException("Hendelse er ikke av type " + no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.HendelseType.periode_startet_v1)
}

fun no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Hendelse.asV3PeriodeAvsluttet(): Hendelse {
    return this.periodeAvsluttetV1?.asV3(HendelseType.PERIODE_AVSLUTTET_V1)
        ?: throw IllegalArgumentException("Hendelse er ikke av type " + no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.HendelseType.periode_avsluttet_v1)
}

fun no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Hendelse.asV3Opplysninger(): Hendelse {
    return this.opplysningerV4?.asV3()
        ?: throw IllegalArgumentException("Hendelse er ikke av type " + no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.HendelseType.opplysninger_v4)
}

fun no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Hendelse.asV3Profilering(): Hendelse {
    return this.profileringV1?.asV3()
        ?: throw IllegalArgumentException("Hendelse er ikke av type " + no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.HendelseType.profilering_v1)
}

fun no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Hendelse.asV3Egenvurdering(): Hendelse {
    return this.egenvurderingV1?.asV3()
        ?: throw IllegalArgumentException("Hendelse er ikke av type " + no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.HendelseType.egenvurdering_v1)
}

fun no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Hendelse.asV3Bekreftelse(): Hendelse {
    return this.bekreftelseV1?.asV3()
        ?: throw IllegalArgumentException("Hendelse er ikke av type " + no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.HendelseType.bekreftelse_v1)
}

fun no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Hendelse.asV3PaaVegneAvStart(): Hendelse {
    return this.paVegneAvStartV1?.asV3()
        ?: throw IllegalArgumentException("Hendelse er ikke av type " + no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.HendelseType.pa_vegne_av_start_v1)
}

fun no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Hendelse.asV3PaaVegneAvStopp(): Hendelse {
    return this.paVegneAvStoppV1?.asV3()
        ?: throw IllegalArgumentException("Hendelse er ikke av type " + no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.HendelseType.pa_vegne_av_stopp_v1)
}

fun no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Metadata.asV3(type: HendelseType): Metadata {
    return Metadata(
        type = type,
        tidspunkt = this.tidspunkt,
        utfoertAv = this.utfoertAv.asV3(),
        kilde = this.kilde,
        aarsak = this.aarsak,
        tidspunktFraKilde = this.tidspunktFraKilde?.asV3()
    )
}

fun no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.OpplysningerOmArbeidssoeker.asV3(): OpplysningerOmArbeidssoeker {
    return OpplysningerOmArbeidssoeker(
        id = this.id,
        sendtInnAv = this.sendtInnAv.asV3(HendelseType.METADATA_V1),
        utdanning = this.utdanning?.asV3(),
        helse = this.helse?.asV3(),
        jobbsituasjon = this.jobbsituasjon?.asV3(),
        annet = this.annet?.asV3()
    )
}

fun no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Utdanning.asV3(): Utdanning {
    return Utdanning(
        nus = this.nus,
        bestaatt = this.bestaatt?.asV3(),
        godkjent = this.godkjent?.asV3(),
    )
}

fun no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Helse.asV3(): Helse {
    return Helse(
        helsetilstandHindrerArbeid = this.helsetilstandHindrerArbeid?.asV3()
    )
}

fun no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Jobbsituasjon.asV3(): Jobbsituasjon {
    return Jobbsituasjon(
        beskrivelser = this.beskrivelser.map { it.asV3() }
    )
}

fun no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.BeskrivelseMedDetaljer.asV3(): BeskrivelseMedDetaljer {
    return BeskrivelseMedDetaljer(
        beskrivelse = this.beskrivelse.asV3(),
        detaljer = this.detaljer
    )
}

fun no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Beskrivelse.asV3(): Beskrivelse {
    return when (this) {
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Beskrivelse.UDEFINERT -> Beskrivelse.UDEFINERT
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Beskrivelse.HAR_SAGT_OPP -> Beskrivelse.HAR_SAGT_OPP
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Beskrivelse.HAR_BLITT_SAGT_OPP -> Beskrivelse.HAR_BLITT_SAGT_OPP
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Beskrivelse.ER_PERMITTERT -> Beskrivelse.ER_PERMITTERT
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Beskrivelse.ALDRI_HATT_JOBB -> Beskrivelse.ALDRI_HATT_JOBB
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Beskrivelse.IKKE_VAERT_I_JOBB_SISTE_2_AAR -> Beskrivelse.IKKE_VAERT_I_JOBB_SISTE_2_AAR
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Beskrivelse.AKKURAT_FULLFORT_UTDANNING -> Beskrivelse.AKKURAT_FULLFORT_UTDANNING
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Beskrivelse.VIL_BYTTE_JOBB -> Beskrivelse.VIL_BYTTE_JOBB
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Beskrivelse.USIKKER_JOBBSITUASJON -> Beskrivelse.USIKKER_JOBBSITUASJON
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Beskrivelse.MIDLERTIDIG_JOBB -> Beskrivelse.MIDLERTIDIG_JOBB
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Beskrivelse.DELTIDSJOBB_VIL_MER -> Beskrivelse.DELTIDSJOBB_VIL_MER
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Beskrivelse.NY_JOBB -> Beskrivelse.NY_JOBB
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Beskrivelse.KONKURS -> Beskrivelse.KONKURS
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Beskrivelse.ANNET -> Beskrivelse.ANNET
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Beskrivelse.UKJENT_VERDI -> Beskrivelse.UKJENT_VERDI
    }
}

fun no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Annet.asV3(): Annet {
    return Annet(
        andreForholdHindrerArbeid = this.andreForholdHindrerArbeid?.asV3()
    )
}

fun no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Profilering.asV3(): Profilering {
    return Profilering(
        id = this.id,
        opplysningerOmArbeidssokerId = this.opplysningerOmArbeidssokerId,
        sendtInnAv = this.sendtInnAv.asV3(HendelseType.METADATA_V1),
        profilertTil = this.profilertTil.asV3(),
        jobbetSammenhengendeSeksAvTolvSisteMnd = this.jobbetSammenhengendeSeksAvTolvSisteMnd,
        alder = this.alder
    )
}

fun no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Egenvurdering.asV3(): Egenvurdering {
    return Egenvurdering(
        id = this.id,
        profileringId = this.profileringId,
        sendtInnAv = this.sendtInnAv.asV3(HendelseType.METADATA_V1),
        profilertTil = this.profilertTil.asV3(),
        egenvurdering = this.egenvurdering.asV3()
    )
}

fun no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.ProfilertTil.asV3(): ProfilertTil {
    return when (this) {
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.ProfilertTil.UDEFINERT -> ProfilertTil.UDEFINERT
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.ProfilertTil.ANTATT_GODE_MULIGHETER -> ProfilertTil.ANTATT_GODE_MULIGHETER
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING -> ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.ProfilertTil.OPPGITT_HINDRINGER -> ProfilertTil.OPPGITT_HINDRINGER
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.ProfilertTil.UKJENT_VERDI -> ProfilertTil.UKJENT_VERDI
    }
}

fun no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.BekreftelseMedMetadata.asV3(): BekreftelseMedMetadata {
    return BekreftelseMedMetadata(
        periodeId = UUID.randomUUID(), // TODO Trengs denne?
        status = this.status?.asV3(),
        bekreftelse = this.bekreftelse?.asV3()
    )
}

fun no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Bekreftelse.asV3(): Bekreftelse {
    return Bekreftelse(
        id = this.id,
        bekreftelsesloesning = this.bekreftelsesloesning.asV3(),
        svar = this.svar.asV3()
    )
}

fun no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Svar.asV3(): Svar {
    return Svar(
        sendtInnAv = this.sendtInnAv.asV3(HendelseType.METADATA_V1),
        gjelderFra = this.gjelderFra,
        gjelderTil = this.gjelderTil,
        harJobbetIDennePerioden = this.harJobbetIDennePerioden,
        vilFortsetteSomArbeidssoeker = this.vilFortsetteSomArbeidssoeker
    )
}

fun no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.BekreftelseMedMetadata.Status.asV3(): BekreftelsStatus {
    return when (this) {
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.BekreftelseMedMetadata.Status.GYLDIG -> BekreftelsStatus.GYLDIG
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.BekreftelseMedMetadata.Status.UVENTET_KILDE -> BekreftelsStatus.UVENTET_KILDE
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.BekreftelseMedMetadata.Status.UTENFOR_PERIODE -> BekreftelsStatus.UTENFOR_PERIODE
    }
}

fun no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.PaaVegneAvStart.asV3(): PaaVegneAvStart {
    return PaaVegneAvStart(
        periodeId = this.periodeId,
        bekreftelsesloesning = this.bekreftelsesloesning.asV3(),
        intervalMS = this.intervalMS,
        graceMS = this.graceMS
    )
}

fun no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.PaaVegneAvStopp.asV3(): PaaVegneAvStopp {
    return PaaVegneAvStopp(
        periodeId = this.periodeId,
        bekreftelsesloesning = this.bekreftelsesloesning.asV3(),
        fristBrutt = this.fristBrutt
    )
}

fun no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Bekreftelsesloesning.asV3(): Bekreftelsesloesning {
    return when (this) {
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET -> Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Bekreftelsesloesning.DAGPENGER -> Bekreftelsesloesning.DAGPENGER
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Bekreftelsesloesning.FRISKMELDT_TIL_ARBEIDSFORMIDLING -> Bekreftelsesloesning.FRISKMELDT_TIL_ARBEIDSFORMIDLING
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Bekreftelsesloesning.UKJENT_VERDI -> Bekreftelsesloesning.UKJENT_VERDI
    }
}

fun no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Bruker.asV3(): Bruker {
    return Bruker(
        type = this.type.asV3(),
        id = this.id,
        sikkerhetsnivaa = this.sikkerhetsnivaa
    )
}

fun no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Bruker.Type.asV3(): BrukerType {
    return when (this) {
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Bruker.Type.SLUTTBRUKER -> BrukerType.SLUTTBRUKER
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Bruker.Type.VEILEDER -> BrukerType.VEILEDER
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Bruker.Type.SYSTEM -> BrukerType.SYSTEM
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Bruker.Type.UDEFINERT -> BrukerType.UDEFINERT
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Bruker.Type.UKJENT_VERDI -> BrukerType.UKJENT_VERDI
    }
}

fun no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.TidspunktFraKilde.asV3(): TidspunktFraKilde {
    return TidspunktFraKilde(
        tidspunkt = this.tidspunkt,
        avviksType = this.avviksType.asV3()
    )
}

fun no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.AvviksType.asV3(): AvviksType {
    return when (this) {
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.AvviksType.FORSINKELSE -> AvviksType.FORSINKELSE
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.AvviksType.RETTING -> AvviksType.RETTING
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.AvviksType.SLETTET -> AvviksType.SLETTET
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.AvviksType.TIDSPUNKT_KORRIGERT -> AvviksType.TIDSPUNKT_KORRIGERT
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.AvviksType.UKJENT_VERDI -> AvviksType.UKJENT_VERDI
    }
}

fun no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.JaNeiVetIkke.asV3(): JaNeiVetIkke {
    return when (this) {
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.JaNeiVetIkke.JA -> JaNeiVetIkke.JA
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.JaNeiVetIkke.NEI -> JaNeiVetIkke.NEI
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.JaNeiVetIkke.VET_IKKE -> JaNeiVetIkke.VET_IKKE
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.JaNeiVetIkke.UKJENT_VERDI -> JaNeiVetIkke.UKJENT_VERDI
    }
}

fun no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.HendelseType.asV3(): HendelseType {
    return when (this) {
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.HendelseType.periode_startet_v1 -> HendelseType.PERIODE_STARTET_V1
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.HendelseType.periode_avsluttet_v1 -> HendelseType.PERIODE_AVSLUTTET_V1
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.HendelseType.opplysninger_v4 -> HendelseType.OPPLYSNINGER_V4
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.HendelseType.profilering_v1 -> HendelseType.PROFILERING_V1
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.HendelseType.egenvurdering_v1 -> HendelseType.EGENVURDERING_V1
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.HendelseType.bekreftelse_v1 -> HendelseType.BEKREFTELSE_V1
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.HendelseType.pa_vegne_av_start_v1 -> HendelseType.PAA_VEGNE_AV_START_V1
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.HendelseType.pa_vegne_av_stopp_v1 -> HendelseType.PAA_VEGNE_AV_STOPP_V1
    }
}