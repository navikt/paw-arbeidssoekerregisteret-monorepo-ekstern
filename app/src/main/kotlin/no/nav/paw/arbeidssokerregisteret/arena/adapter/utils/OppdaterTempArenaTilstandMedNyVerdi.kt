package no.nav.paw.arbeidssokerregisteret.arena.adapter.utils

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.arena.adapter.toArena
import no.nav.paw.arbeidssokerregisteret.arena.helpers.v4.TopicsJoin
import no.nav.paw.arbeidssokerregisteret.arena.v2.Annet
import no.nav.paw.arbeidssokerregisteret.arena.v1.Helse
import org.apache.avro.specific.SpecificRecord

fun oppdaterTempArenaTilstandMedNyVerdi(
    nyVerdi: SpecificRecord?,
    gjeldeneTilstand: TopicsJoin
): TopicsJoin {
    val periode = (nyVerdi as? Periode)
        ?.let { periode ->
            if (gjeldeneTilstand.periode == null || periode.avsluttet != null) {
                toArena(periode)
            } else {
                gjeldeneTilstand.periode
            }
        } ?: gjeldeneTilstand.periode
    val profilering = (nyVerdi as? Profilering)
        ?.let { nyProfilering -> gjeldeneTilstand.profilering ?: toArena(nyProfilering) }
        ?: gjeldeneTilstand.profilering
    val opplysninger = (nyVerdi as? OpplysningerOmArbeidssoeker)
        ?.let { nyeOpplysninger -> gjeldeneTilstand.opplysningerOmArbeidssoeker ?: toArena(nyeOpplysninger) }
        ?: gjeldeneTilstand.opplysningerOmArbeidssoeker
    val erAvsluttet = periode?.avsluttet != null
    return if (erAvsluttet) {
        TopicsJoin(
            periode,
            null,
            null
        )
    } else {
        TopicsJoin(
            periode,
            profilering,
            opplysninger
        )
    }
}

private fun toArena(opplysningerOmArbeidssoeker: OpplysningerOmArbeidssoeker) =
    no.nav.paw.arbeidssokerregisteret.arena.v4.OpplysningerOmArbeidssoeker(
        opplysningerOmArbeidssoeker.id,
        opplysningerOmArbeidssoeker.periodeId,
        opplysningerOmArbeidssoeker.sendtInnAv.toArena(),
        opplysningerOmArbeidssoeker.utdanning?.toArena(),
        opplysningerOmArbeidssoeker.helse?.toArena(),
        opplysningerOmArbeidssoeker.jobbsituasjon.toArena(),
        opplysningerOmArbeidssoeker.annet?.toArena()
    )

private fun toArena(profilering: Profilering) =
    no.nav.paw.arbeidssokerregisteret.arena.v1.Profilering(
        profilering.id,
        profilering.periodeId,
        profilering.opplysningerOmArbeidssokerId,
        profilering.sendtInnAv.toArena(),
        profilering.profilertTil.toArena(),
        profilering.jobbetSammenhengendeSeksAvTolvSisteMnd,
        profilering.alder
    )

private fun toArena(periode: Periode) =
    no.nav.paw.arbeidssokerregisteret.arena.v1.Periode(
        periode.id,
        periode.identitetsnummer,
        periode.startet.toArena(),
        periode.avsluttet?.toArena()
    )
