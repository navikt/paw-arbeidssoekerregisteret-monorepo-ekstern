package no.nav.paw.oppslagapi.data.query

import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Egenvurdering
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Hendelse
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.HendelseType
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Metadata
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.PaaVegneAvStart
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.PaaVegneAvStopp
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Profilering
import no.nav.paw.oppslagapi.data.Row
import no.nav.paw.oppslagapi.data.egenvurdering_v1
import no.nav.paw.oppslagapi.data.opplysninger_om_arbeidssoeker_v4
import no.nav.paw.oppslagapi.data.pa_vegne_av_start_v1
import no.nav.paw.oppslagapi.data.pa_vegne_av_stopp_v1
import no.nav.paw.oppslagapi.data.periode_avsluttet_v1
import no.nav.paw.oppslagapi.data.periode_startet_v1
import no.nav.paw.oppslagapi.data.profilering_v1

/**
 * Mapper alt untatt bekreftelsemelding_v1 til Hendelse.
 * Bekreftelsemelding_v1 håndteres separat i lagTidslinjer.
 */
fun mapIkkeBekreftelseRaderTilHendelser(rad: Row<out Any>): Hendelse = when (rad.type) {
    periode_startet_v1 -> Hendelse(
        hendelseType = HendelseType.periode_startet_v1,
        tidspunkt = rad.timestamp,
        periodeStartetV1 = rad.data as Metadata
    )

    periode_avsluttet_v1 -> Hendelse(
        hendelseType = HendelseType.periode_avsluttet_v1,
        tidspunkt = rad.timestamp,
        periodeAvsluttetV1 = rad.data as Metadata
    )

    pa_vegne_av_start_v1 -> Hendelse(
        hendelseType = HendelseType.pa_vegne_av_start_v1,
        tidspunkt = rad.timestamp,
        paVegneAvStartV1 = rad.data as PaaVegneAvStart
    )

    pa_vegne_av_stopp_v1 -> Hendelse(
        hendelseType = HendelseType.pa_vegne_av_stopp_v1,
        tidspunkt = rad.timestamp,
        paVegneAvStoppV1 = rad.data as PaaVegneAvStopp
    )

    opplysninger_om_arbeidssoeker_v4 -> Hendelse(
        hendelseType = HendelseType.opplysninger_v4,
        tidspunkt = rad.timestamp,
        opplysningerV4 = rad.data as OpplysningerOmArbeidssoeker
    )

    profilering_v1 -> Hendelse(
        hendelseType = HendelseType.profilering_v1,
        tidspunkt = rad.timestamp,
        profileringV1 = rad.data as Profilering
    )

    egenvurdering_v1 -> Hendelse(
        hendelseType = HendelseType.egenvurdering_v1,
        tidspunkt = rad.timestamp,
        egenvurderingV1 = rad.data as Egenvurdering
    )

    else -> throw IllegalArgumentException("Ukjent hendelsestype: ${rad.type}")
}