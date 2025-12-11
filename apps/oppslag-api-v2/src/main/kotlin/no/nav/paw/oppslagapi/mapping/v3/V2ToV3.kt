package no.nav.paw.oppslagapi.mapping.v3

import no.nav.paw.oppslagapi.model.v3.Annet
import no.nav.paw.oppslagapi.model.v3.AvviksType
import no.nav.paw.oppslagapi.model.v3.BekreftelsStatus
import no.nav.paw.oppslagapi.model.v3.Bekreftelse
import no.nav.paw.oppslagapi.model.v3.Bekreftelsesloesning
import no.nav.paw.oppslagapi.model.v3.Beskrivelse
import no.nav.paw.oppslagapi.model.v3.BeskrivelseMedDetaljer
import no.nav.paw.oppslagapi.model.v3.Bruker
import no.nav.paw.oppslagapi.model.v3.BrukerType
import no.nav.paw.oppslagapi.model.v3.Egenvurdering
import no.nav.paw.oppslagapi.model.v3.Helse
import no.nav.paw.oppslagapi.model.v3.Hendelse
import no.nav.paw.oppslagapi.model.v3.JaNeiVetIkke
import no.nav.paw.oppslagapi.model.v3.Jobbsituasjon
import no.nav.paw.oppslagapi.model.v3.Metadata
import no.nav.paw.oppslagapi.model.v3.OpplysningerOmArbeidssoeker
import no.nav.paw.oppslagapi.model.v3.PaaVegneAvStart
import no.nav.paw.oppslagapi.model.v3.PaaVegneAvStopp
import no.nav.paw.oppslagapi.model.v3.PeriodeAvluttet
import no.nav.paw.oppslagapi.model.v3.PeriodeStartet
import no.nav.paw.oppslagapi.model.v3.Profilering
import no.nav.paw.oppslagapi.model.v3.ProfilertTil
import no.nav.paw.oppslagapi.model.v3.Svar
import no.nav.paw.oppslagapi.model.v3.Tidslinje
import no.nav.paw.oppslagapi.model.v3.TidspunktFraKilde
import no.nav.paw.oppslagapi.model.v3.Utdanning
import java.time.Instant

fun no.nav.paw.oppslagapi.model.v2.Tidslinje.asV3(): Tidslinje {
    return Tidslinje(
        periodeId = this.periodeId,
        identitetsnummer = this.identitetsnummer,
        startet = this.startet,
        avsluttet = this.avsluttet,
        hendelser = this.hendelser.mapNotNull { it.asV3() }
    )
}

fun no.nav.paw.oppslagapi.model.v2.Hendelse.asV3(): Hendelse? {
    return when (this.hendelseType) {
        no.nav.paw.oppslagapi.model.v2.HendelseType.periode_startet_v1 -> this.asV3PeriodeStartet()
        no.nav.paw.oppslagapi.model.v2.HendelseType.periode_avsluttet_v1 -> this.asV3PeriodeAvsluttet()
        no.nav.paw.oppslagapi.model.v2.HendelseType.opplysninger_v4 -> this.asV3Opplysninger()
        no.nav.paw.oppslagapi.model.v2.HendelseType.profilering_v1 -> this.asV3Profilering()
        no.nav.paw.oppslagapi.model.v2.HendelseType.egenvurdering_v1 -> this.asV3Egenvurdering()
        no.nav.paw.oppslagapi.model.v2.HendelseType.bekreftelse_v1 -> this.asV3Bekreftelse()
        no.nav.paw.oppslagapi.model.v2.HendelseType.pa_vegne_av_start_v1 -> this.asV3PaaVegneAvStart()
        no.nav.paw.oppslagapi.model.v2.HendelseType.pa_vegne_av_stopp_v1 -> this.asV3PaaVegneAvStopp()
    }
}

fun no.nav.paw.oppslagapi.model.v2.Hendelse.asV3PeriodeStartet(): Hendelse {
    return this.periodeStartetV1?.asV3PeriodeStartet(this.tidspunkt)
        ?: throw IllegalArgumentException("Hendelse er ikke av type " + no.nav.paw.oppslagapi.model.v2.HendelseType.periode_startet_v1)
}

fun no.nav.paw.oppslagapi.model.v2.Hendelse.asV3PeriodeAvsluttet(): Hendelse {
    return this.periodeAvsluttetV1?.asV3PeriodeAvsluttet(this.tidspunkt)
        ?: throw IllegalArgumentException("Hendelse er ikke av type " + no.nav.paw.oppslagapi.model.v2.HendelseType.periode_avsluttet_v1)
}

fun no.nav.paw.oppslagapi.model.v2.Hendelse.asV3Opplysninger(): Hendelse {
    return this.opplysningerV4?.asV3(this.tidspunkt)
        ?: throw IllegalArgumentException("Hendelse er ikke av type " + no.nav.paw.oppslagapi.model.v2.HendelseType.opplysninger_v4)
}

fun no.nav.paw.oppslagapi.model.v2.Hendelse.asV3Profilering(): Hendelse {
    return this.profileringV1?.asV3(this.tidspunkt)
        ?: throw IllegalArgumentException("Hendelse er ikke av type " + no.nav.paw.oppslagapi.model.v2.HendelseType.profilering_v1)
}

fun no.nav.paw.oppslagapi.model.v2.Hendelse.asV3Egenvurdering(): Hendelse {
    return this.egenvurderingV1?.asV3(this.tidspunkt)
        ?: throw IllegalArgumentException("Hendelse er ikke av type " + no.nav.paw.oppslagapi.model.v2.HendelseType.egenvurdering_v1)
}

fun no.nav.paw.oppslagapi.model.v2.Hendelse.asV3Bekreftelse(): Hendelse? {
    return this.bekreftelseV1?.asV3(this.tidspunkt)
}

fun no.nav.paw.oppslagapi.model.v2.Hendelse.asV3PaaVegneAvStart(): Hendelse {
    return this.paVegneAvStartV1?.asV3(this.tidspunkt)
        ?: throw IllegalArgumentException("Hendelse er ikke av type " + no.nav.paw.oppslagapi.model.v2.HendelseType.pa_vegne_av_start_v1)
}

fun no.nav.paw.oppslagapi.model.v2.Hendelse.asV3PaaVegneAvStopp(
): Hendelse {
    return this.paVegneAvStoppV1?.asV3(this.tidspunkt)
        ?: throw IllegalArgumentException("Hendelse er ikke av type " + no.nav.paw.oppslagapi.model.v2.HendelseType.pa_vegne_av_stopp_v1)
}

fun no.nav.paw.oppslagapi.model.v2.Metadata.asV3(): Metadata {
    return Metadata(
        tidspunkt = this.tidspunkt,
        utfoertAv = this.utfoertAv.asV3(),
        kilde = this.kilde,
        aarsak = this.aarsak,
        tidspunktFraKilde = this.tidspunktFraKilde?.asV3()
    )
}

fun no.nav.paw.oppslagapi.model.v2.Metadata.asV3PeriodeStartet(tidspunkt: Instant): PeriodeStartet {
    return PeriodeStartet(
        sendtInnAv = this.asV3(),
        tidspunkt = tidspunkt
    )
}

fun no.nav.paw.oppslagapi.model.v2.Metadata.asV3PeriodeAvsluttet(tidspunkt: Instant): PeriodeAvluttet {
    return PeriodeAvluttet(
        sendtInnAv = this.asV3(),
        tidspunkt = tidspunkt
    )
}

fun no.nav.paw.oppslagapi.model.v2.OpplysningerOmArbeidssoeker.asV3(tidspunkt: Instant): OpplysningerOmArbeidssoeker {
    return OpplysningerOmArbeidssoeker(
        id = this.id,
        sendtInnAv = this.sendtInnAv.asV3(),
        utdanning = this.utdanning?.asV3(),
        helse = this.helse?.asV3(),
        jobbsituasjon = this.jobbsituasjon?.asV3(),
        annet = this.annet?.asV3(),
        tidspunkt = tidspunkt
    )
}

fun no.nav.paw.oppslagapi.model.v2.Utdanning.asV3(): Utdanning {
    return Utdanning(
        nus = this.nus,
        bestaatt = this.bestaatt?.asV3(),
        godkjent = this.godkjent?.asV3(),
    )
}

fun no.nav.paw.oppslagapi.model.v2.Helse.asV3(): Helse {
    return Helse(
        helsetilstandHindrerArbeid = this.helsetilstandHindrerArbeid?.asV3()
    )
}

fun no.nav.paw.oppslagapi.model.v2.Jobbsituasjon.asV3(): Jobbsituasjon {
    return Jobbsituasjon(
        beskrivelser = this.beskrivelser.map { it.asV3() }
    )
}

fun no.nav.paw.oppslagapi.model.v2.BeskrivelseMedDetaljer.asV3(): BeskrivelseMedDetaljer {
    return BeskrivelseMedDetaljer(
        beskrivelse = this.beskrivelse.asV3(),
        detaljer = this.detaljer
    )
}

fun no.nav.paw.oppslagapi.model.v2.Beskrivelse.asV3(): Beskrivelse {
    return when (this) {
        no.nav.paw.oppslagapi.model.v2.Beskrivelse.UDEFINERT -> Beskrivelse.UDEFINERT
        no.nav.paw.oppslagapi.model.v2.Beskrivelse.HAR_SAGT_OPP -> Beskrivelse.HAR_SAGT_OPP
        no.nav.paw.oppslagapi.model.v2.Beskrivelse.HAR_BLITT_SAGT_OPP -> Beskrivelse.HAR_BLITT_SAGT_OPP
        no.nav.paw.oppslagapi.model.v2.Beskrivelse.ER_PERMITTERT -> Beskrivelse.ER_PERMITTERT
        no.nav.paw.oppslagapi.model.v2.Beskrivelse.ALDRI_HATT_JOBB -> Beskrivelse.ALDRI_HATT_JOBB
        no.nav.paw.oppslagapi.model.v2.Beskrivelse.IKKE_VAERT_I_JOBB_SISTE_2_AAR -> Beskrivelse.IKKE_VAERT_I_JOBB_SISTE_2_AAR
        no.nav.paw.oppslagapi.model.v2.Beskrivelse.AKKURAT_FULLFORT_UTDANNING -> Beskrivelse.AKKURAT_FULLFORT_UTDANNING
        no.nav.paw.oppslagapi.model.v2.Beskrivelse.VIL_BYTTE_JOBB -> Beskrivelse.VIL_BYTTE_JOBB
        no.nav.paw.oppslagapi.model.v2.Beskrivelse.USIKKER_JOBBSITUASJON -> Beskrivelse.USIKKER_JOBBSITUASJON
        no.nav.paw.oppslagapi.model.v2.Beskrivelse.MIDLERTIDIG_JOBB -> Beskrivelse.MIDLERTIDIG_JOBB
        no.nav.paw.oppslagapi.model.v2.Beskrivelse.DELTIDSJOBB_VIL_MER -> Beskrivelse.DELTIDSJOBB_VIL_MER
        no.nav.paw.oppslagapi.model.v2.Beskrivelse.NY_JOBB -> Beskrivelse.NY_JOBB
        no.nav.paw.oppslagapi.model.v2.Beskrivelse.KONKURS -> Beskrivelse.KONKURS
        no.nav.paw.oppslagapi.model.v2.Beskrivelse.ANNET -> Beskrivelse.ANNET
        no.nav.paw.oppslagapi.model.v2.Beskrivelse.UKJENT_VERDI -> Beskrivelse.UKJENT_VERDI
    }
}

fun no.nav.paw.oppslagapi.model.v2.Annet.asV3(): Annet {
    return Annet(
        andreForholdHindrerArbeid = this.andreForholdHindrerArbeid?.asV3()
    )
}

fun no.nav.paw.oppslagapi.model.v2.Profilering.asV3(tidspunkt: Instant): Profilering {
    return Profilering(
        id = this.id,
        opplysningerOmArbeidssokerId = this.opplysningerOmArbeidssokerId,
        sendtInnAv = this.sendtInnAv.asV3(),
        profilertTil = this.profilertTil.asV3(),
        jobbetSammenhengendeSeksAvTolvSisteMnd = this.jobbetSammenhengendeSeksAvTolvSisteMnd,
        alder = this.alder,
        tidspunkt = tidspunkt
    )
}

fun no.nav.paw.oppslagapi.model.v2.Egenvurdering.asV3(tidspunkt: Instant): Egenvurdering {
    return Egenvurdering(
        id = this.id,
        profileringId = this.profileringId,
        sendtInnAv = this.sendtInnAv.asV3(),
        profilertTil = this.profilertTil.asV3(),
        egenvurdering = this.egenvurdering.asV3(),
        tidspunkt = tidspunkt
    )
}

fun no.nav.paw.oppslagapi.model.v2.ProfilertTil.asV3(): ProfilertTil {
    return when (this) {
        no.nav.paw.oppslagapi.model.v2.ProfilertTil.UDEFINERT -> ProfilertTil.UDEFINERT
        no.nav.paw.oppslagapi.model.v2.ProfilertTil.ANTATT_GODE_MULIGHETER -> ProfilertTil.ANTATT_GODE_MULIGHETER
        no.nav.paw.oppslagapi.model.v2.ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING -> ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
        no.nav.paw.oppslagapi.model.v2.ProfilertTil.OPPGITT_HINDRINGER -> ProfilertTil.OPPGITT_HINDRINGER
        no.nav.paw.oppslagapi.model.v2.ProfilertTil.UKJENT_VERDI -> ProfilertTil.UKJENT_VERDI
    }
}

fun no.nav.paw.oppslagapi.model.v2.BekreftelseMedMetadata.asV3(tidspunkt: Instant): Bekreftelse? {
    val status = this.status?.asV3() ?: BekreftelsStatus.GYLDIG
    return if (status == BekreftelsStatus.GYLDIG && this.bekreftelse != null) {
        Bekreftelse(
            id = this.bekreftelse.id,
            bekreftelsesloesning = this.bekreftelse.bekreftelsesloesning.asV3(),
            status = status,
            svar = this.bekreftelse.svar.asV3(),
            tidspunkt = tidspunkt
        )
    } else {
        null
    }
}

fun no.nav.paw.oppslagapi.model.v2.Svar.asV3(): Svar {
    return Svar(
        sendtInnAv = this.sendtInnAv.asV3(),
        gjelderFra = this.gjelderFra,
        gjelderTil = this.gjelderTil,
        harJobbetIDennePerioden = this.harJobbetIDennePerioden,
        vilFortsetteSomArbeidssoeker = this.vilFortsetteSomArbeidssoeker
    )
}

fun no.nav.paw.oppslagapi.model.v2.BekreftelseStatus.asV3(): BekreftelsStatus {
    return when (this) {
        no.nav.paw.oppslagapi.model.v2.BekreftelseStatus.GYLDIG -> BekreftelsStatus.GYLDIG
        no.nav.paw.oppslagapi.model.v2.BekreftelseStatus.UVENTET_KILDE -> BekreftelsStatus.UVENTET_KILDE
        no.nav.paw.oppslagapi.model.v2.BekreftelseStatus.UTENFOR_PERIODE -> BekreftelsStatus.UTENFOR_PERIODE
    }
}

fun no.nav.paw.oppslagapi.model.v2.PaaVegneAvStart.asV3(tidspunkt: Instant): PaaVegneAvStart {
    return PaaVegneAvStart(
        periodeId = this.periodeId,
        bekreftelsesloesning = this.bekreftelsesloesning.asV3(),
        intervalMS = this.intervalMS,
        graceMS = this.graceMS,
        tidspunkt = tidspunkt
    )
}

fun no.nav.paw.oppslagapi.model.v2.PaaVegneAvStopp.asV3(tidspunkt: Instant): PaaVegneAvStopp {
    return PaaVegneAvStopp(
        periodeId = this.periodeId,
        bekreftelsesloesning = this.bekreftelsesloesning.asV3(),
        fristBrutt = this.fristBrutt,
        tidspunkt = tidspunkt
    )
}

fun no.nav.paw.oppslagapi.model.v2.Bekreftelsesloesning.asV3(): Bekreftelsesloesning {
    return when (this) {
        no.nav.paw.oppslagapi.model.v2.Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET -> Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET
        no.nav.paw.oppslagapi.model.v2.Bekreftelsesloesning.DAGPENGER -> Bekreftelsesloesning.DAGPENGER
        no.nav.paw.oppslagapi.model.v2.Bekreftelsesloesning.FRISKMELDT_TIL_ARBEIDSFORMIDLING -> Bekreftelsesloesning.FRISKMELDT_TIL_ARBEIDSFORMIDLING
        no.nav.paw.oppslagapi.model.v2.Bekreftelsesloesning.UKJENT_VERDI -> Bekreftelsesloesning.UKJENT_VERDI
    }
}

fun no.nav.paw.oppslagapi.model.v2.Bruker.asV3(): Bruker {
    return Bruker(
        type = this.type.asV3(),
        id = this.id,
        sikkerhetsnivaa = this.sikkerhetsnivaa
    )
}

fun no.nav.paw.oppslagapi.model.v2.BrukerType.asV3(): BrukerType {
    return when (this) {
        no.nav.paw.oppslagapi.model.v2.BrukerType.SLUTTBRUKER -> BrukerType.SLUTTBRUKER
        no.nav.paw.oppslagapi.model.v2.BrukerType.VEILEDER -> BrukerType.VEILEDER
        no.nav.paw.oppslagapi.model.v2.BrukerType.SYSTEM -> BrukerType.SYSTEM
        no.nav.paw.oppslagapi.model.v2.BrukerType.UDEFINERT -> BrukerType.UDEFINERT
        no.nav.paw.oppslagapi.model.v2.BrukerType.UKJENT_VERDI -> BrukerType.UKJENT_VERDI
    }
}

fun no.nav.paw.oppslagapi.model.v2.TidspunktFraKilde.asV3(): TidspunktFraKilde {
    return TidspunktFraKilde(
        tidspunkt = this.tidspunkt,
        avviksType = this.avviksType.asV3()
    )
}

fun no.nav.paw.oppslagapi.model.v2.AvviksType.asV3(): AvviksType {
    return when (this) {
        no.nav.paw.oppslagapi.model.v2.AvviksType.FORSINKELSE -> AvviksType.FORSINKELSE
        no.nav.paw.oppslagapi.model.v2.AvviksType.RETTING -> AvviksType.RETTING
        no.nav.paw.oppslagapi.model.v2.AvviksType.SLETTET -> AvviksType.SLETTET
        no.nav.paw.oppslagapi.model.v2.AvviksType.TIDSPUNKT_KORRIGERT -> AvviksType.TIDSPUNKT_KORRIGERT
        no.nav.paw.oppslagapi.model.v2.AvviksType.UKJENT_VERDI -> AvviksType.UKJENT_VERDI
    }
}

fun no.nav.paw.oppslagapi.model.v2.JaNeiVetIkke.asV3(): JaNeiVetIkke {
    return when (this) {
        no.nav.paw.oppslagapi.model.v2.JaNeiVetIkke.JA -> JaNeiVetIkke.JA
        no.nav.paw.oppslagapi.model.v2.JaNeiVetIkke.NEI -> JaNeiVetIkke.NEI
        no.nav.paw.oppslagapi.model.v2.JaNeiVetIkke.VET_IKKE -> JaNeiVetIkke.VET_IKKE
        no.nav.paw.oppslagapi.model.v2.JaNeiVetIkke.UKJENT_VERDI -> JaNeiVetIkke.UKJENT_VERDI
    }
}