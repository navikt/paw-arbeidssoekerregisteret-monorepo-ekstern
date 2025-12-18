package no.nav.paw.oppslagapi.mapping.v4

import no.nav.paw.oppslagapi.mapping.v3.asV3
import no.nav.paw.oppslagapi.model.v2.BekreftelseMedMetadata
import no.nav.paw.oppslagapi.model.v2.Egenvurdering
import no.nav.paw.oppslagapi.model.v2.Hendelse
import no.nav.paw.oppslagapi.model.v2.HendelseType
import no.nav.paw.oppslagapi.model.v2.Metadata
import no.nav.paw.oppslagapi.model.v2.OpplysningerOmArbeidssoeker
import no.nav.paw.oppslagapi.model.v2.PaaVegneAvStart
import no.nav.paw.oppslagapi.model.v2.PaaVegneAvStopp
import no.nav.paw.oppslagapi.model.v2.Profilering
import no.nav.paw.oppslagapi.model.v2.Tidslinje
import no.nav.paw.oppslagapi.model.v3.BekreftelsStatus
import no.nav.paw.oppslagapi.model.v4.BekreftelseV4
import no.nav.paw.oppslagapi.model.v4.EgenvurderingV4
import no.nav.paw.oppslagapi.model.v4.HendelseV4
import no.nav.paw.oppslagapi.model.v4.OpplysningerOmArbeidssoekerV4
import no.nav.paw.oppslagapi.model.v4.PaaVegneAvStartV4
import no.nav.paw.oppslagapi.model.v4.PaaVegneAvStoppV4
import no.nav.paw.oppslagapi.model.v4.PeriodeAvluttetV4
import no.nav.paw.oppslagapi.model.v4.PeriodeStartetV4
import no.nav.paw.oppslagapi.model.v4.ProfileringV4
import no.nav.paw.oppslagapi.model.v4.TidslinjeV4

fun Tidslinje.asV4(): TidslinjeV4 {
    return TidslinjeV4(
        periodeId = this.periodeId,
        identitetsnummer = this.identitetsnummer,
        startet = this.startet,
        avsluttet = this.avsluttet,
        hendelser = this.hendelser.mapNotNull { it.asV4() }
    )
}

fun Hendelse.asV4(): HendelseV4? {
    return when (this.hendelseType) {
        HendelseType.periode_startet_v1 -> this.asV4PeriodeStartet()
        HendelseType.periode_avsluttet_v1 -> this.asV4PeriodeAvsluttet()
        HendelseType.opplysninger_v4 -> this.asV4Opplysninger()
        HendelseType.profilering_v1 -> this.asV4Profilering()
        HendelseType.egenvurdering_v1 -> this.asV4Egenvurdering()
        HendelseType.bekreftelse_v1 -> this.asV4Bekreftelse()
        HendelseType.pa_vegne_av_start_v1 -> this.asV4PaaVegneAvStart()
        HendelseType.pa_vegne_av_stopp_v1 -> this.asV4PaaVegneAvStopp()
    }
}

fun Hendelse.asV4PeriodeStartet(): HendelseV4 {
    return this.periodeStartetV1?.asV4PeriodeStartet()
        ?: throw IllegalArgumentException("Hendelse er ikke av type " + HendelseType.periode_startet_v1)
}

fun Hendelse.asV4PeriodeAvsluttet(): HendelseV4 {
    return this.periodeAvsluttetV1?.asV4PeriodeAvsluttet()
        ?: throw IllegalArgumentException("Hendelse er ikke av type " + HendelseType.periode_avsluttet_v1)
}

fun Hendelse.asV4Opplysninger(): HendelseV4 {
    return this.opplysningerV4?.asV4()
        ?: throw IllegalArgumentException("Hendelse er ikke av type " + HendelseType.opplysninger_v4)
}

fun Hendelse.asV4Profilering(): HendelseV4 {
    return this.profileringV1?.asV4()
        ?: throw IllegalArgumentException("Hendelse er ikke av type " + HendelseType.profilering_v1)
}

fun Hendelse.asV4Egenvurdering(): HendelseV4 {
    return this.egenvurderingV1?.asV4()
        ?: throw IllegalArgumentException("Hendelse er ikke av type " + HendelseType.egenvurdering_v1)
}

fun Hendelse.asV4Bekreftelse(): HendelseV4? {
    return this.bekreftelseV1?.asV4()
}

fun Hendelse.asV4PaaVegneAvStart(): HendelseV4 {
    return this.paVegneAvStartV1?.asV4()
        ?: throw IllegalArgumentException("Hendelse er ikke av type " + HendelseType.pa_vegne_av_start_v1)
}

fun Hendelse.asV4PaaVegneAvStopp(): HendelseV4 {
    return this.paVegneAvStoppV1?.asV4()
        ?: throw IllegalArgumentException("Hendelse er ikke av type " + HendelseType.pa_vegne_av_stopp_v1)
}

fun Metadata.asV4PeriodeStartet(): PeriodeStartetV4 {
    return PeriodeStartetV4(
        tidspunkt = this.tidspunkt,
        utfoertAv = this.utfoertAv.asV3(),
        kilde = this.kilde,
        aarsak = this.aarsak,
        tidspunktFraKilde = this.tidspunktFraKilde?.asV3()
    )
}

fun Metadata.asV4PeriodeAvsluttet(): PeriodeAvluttetV4 {
    return PeriodeAvluttetV4(
        tidspunkt = this.tidspunkt,
        utfoertAv = this.utfoertAv.asV3(),
        kilde = this.kilde,
        aarsak = this.aarsak,
        tidspunktFraKilde = this.tidspunktFraKilde?.asV3()
    )
}

fun OpplysningerOmArbeidssoeker.asV4(): OpplysningerOmArbeidssoekerV4 {
    return OpplysningerOmArbeidssoekerV4(
        id = this.id,
        sendtInnAv = this.sendtInnAv.asV3(),
        utdanning = this.utdanning?.asV3(),
        helse = this.helse?.asV3(),
        jobbsituasjon = this.jobbsituasjon?.asV3(),
        annet = this.annet?.asV3()
    )
}

fun Profilering.asV4(): ProfileringV4 {
    return ProfileringV4(
        id = this.id,
        opplysningerOmArbeidssokerId = this.opplysningerOmArbeidssokerId,
        sendtInnAv = this.sendtInnAv.asV3(),
        profilertTil = this.profilertTil.asV3(),
        jobbetSammenhengendeSeksAvTolvSisteMnd = this.jobbetSammenhengendeSeksAvTolvSisteMnd,
        alder = this.alder
    )
}

fun Egenvurdering.asV4(): EgenvurderingV4 {
    return EgenvurderingV4(
        id = this.id,
        profileringId = this.profileringId,
        sendtInnAv = this.sendtInnAv.asV3(),
        profilertTil = this.profilertTil.asV3(),
        egenvurdering = this.egenvurdering.asV3()
    )
}


fun BekreftelseMedMetadata.asV4(): BekreftelseV4? {
    val status = this.status?.asV3() ?: BekreftelsStatus.GYLDIG
    return if (status == BekreftelsStatus.GYLDIG && this.bekreftelse != null) {
        BekreftelseV4(
            id = this.bekreftelse.id,
            bekreftelsesloesning = this.bekreftelse.bekreftelsesloesning.asV3(),
            status = status,
            svar = this.bekreftelse.svar.asV3()
        )
    } else {
        null
    }
}

fun PaaVegneAvStart.asV4(): PaaVegneAvStartV4 {
    return PaaVegneAvStartV4(
        periodeId = this.periodeId,
        bekreftelsesloesning = this.bekreftelsesloesning.asV3(),
        intervalMS = this.intervalMS,
        graceMS = this.graceMS
    )
}

fun PaaVegneAvStopp.asV4(): PaaVegneAvStoppV4 {
    return PaaVegneAvStoppV4(
        periodeId = this.periodeId,
        bekreftelsesloesning = this.bekreftelsesloesning.asV3(),
        fristBrutt = this.fristBrutt
    )
}
