package no.nav.paw.arbeidssokerregisteret.profilering.application.profilering

import no.nav.paw.arbeidssokerregisteret.api.v1.AvviksType
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import no.nav.paw.arbeidssokerregisteret.api.v1.TidspunktFraKilde
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.config.env.appImageOrDefaultForLocal
import no.nav.paw.config.env.currentRuntimeEnvironment
import java.time.Instant
import java.util.*

fun profileringsResultat(
    alder: Int?,
    opplysninger: OpplysningerOmArbeidssoeker,
    profilering: ProfilertTil,
    harJobbetSammenhengende6AvSiste12Mnd: Boolean,
    profileringTidspunkt: Instant,
    tidspunktFraKilde: Instant
): Profilering {
    return Profilering(
        UUID.randomUUID(),
        opplysninger.periodeId,
        opplysninger.id,
        Metadata(
            profileringTidspunkt,
            bruker,
            currentRuntimeEnvironment.appImageOrDefaultForLocal(),
            "opplysninger-mottatt",
            TidspunktFraKilde(
                tidspunktFraKilde,
                AvviksType.FORSINKELSE
            )
        ),
        profilering,
        harJobbetSammenhengende6AvSiste12Mnd,
        alder
    )
}

val bruker = Bruker(
    BrukerType.SYSTEM,
    currentRuntimeEnvironment.appImageOrDefaultForLocal()
)
