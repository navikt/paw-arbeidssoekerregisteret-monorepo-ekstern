package no.nav.paw.oppslagapi.data

import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Bekreftelse
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Metadata
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.PaaVegneAvStart
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.PaaVegneAvStopp
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Profilering

const val periode_startet_v1 = "periode_startet-v1"
const val periode_avsluttet_v1 = "periode_avsluttet-v1"
const val opplysninger_om_arbeidssoeker_v4 = "opplysninger-v4"
const val profilering_v1 = "profilering-v1"
const val bekreftelsemelding_v1 = "bekreftelse-v1"
const val pa_vegne_av_start_v1 = "pa_vegne_av_start-v1"
const val pa_vegne_av_stopp_v1 = "pa_vegne_av_stopp-v1"

val typeTilKlasse = mapOf(
    periode_startet_v1 to Metadata::class,
    periode_avsluttet_v1 to Metadata::class,
    opplysninger_om_arbeidssoeker_v4 to OpplysningerOmArbeidssoeker::class,
    profilering_v1 to Profilering::class,
    bekreftelsemelding_v1 to Bekreftelse::class,
    pa_vegne_av_start_v1 to PaaVegneAvStart::class,
    pa_vegne_av_stopp_v1 to PaaVegneAvStopp::class
)