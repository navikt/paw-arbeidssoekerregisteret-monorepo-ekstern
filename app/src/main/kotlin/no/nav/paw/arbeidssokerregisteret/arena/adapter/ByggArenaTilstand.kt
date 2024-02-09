package no.nav.paw.arbeidssokerregisteret.arena.adapter

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v3.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.arena.v1.Annet
import no.nav.paw.arbeidssokerregisteret.arena.v1.Arbeidserfaring
import no.nav.paw.arbeidssokerregisteret.arena.v1.Helse
import no.nav.paw.arbeidssokerregisteret.arena.v3.ArenaArbeidssokerregisterTilstand

fun byggArenaTilstand(
    periode: Periode,
    opplysninger: OpplysningerOmArbeidssoeker,
    profilering: Profilering
): ArenaArbeidssokerregisterTilstand {
    val periode = no.nav.paw.arbeidssokerregisteret.arena.v1.Periode(
        periode.id,
        periode.identitetsnummer,
        periode.startet.toArena(),
        periode.avsluttet.toArena()
    )
    val opplysninger = no.nav.paw.arbeidssokerregisteret.arena.v3.OpplysningerOmArbeidssoeker(
        opplysninger.id,
        opplysninger.periodeId,
        opplysninger.sendtInnAv.toArena(),
        opplysninger.utdanning.toArena(),
        Helse(opplysninger.helse.helsetilstandHindrerArbeid.toArena()),
        Arbeidserfaring(opplysninger.arbeidserfaring.harHattArbeid.toArena()),
        opplysninger.jobbsituasjon.toArena(),
        Annet(opplysninger.annet.andreForholdHindrerArbeid.toArena())
    )
    val profilering = no.nav.paw.arbeidssokerregisteret.arena.v1.Profilering(
        profilering.id,
        profilering.periodeId,
        profilering.opplysningerOmArbeidssokerId,
        profilering.sendtInnAv.toArena(),
        profilering.profilertTil.toArena(),
        profilering.jobbetSammenhengendeSeksAvTolvSisteMnd,
        profilering.alder
    )
    return ArenaArbeidssokerregisterTilstand(
        periode,
        profilering,
        opplysninger
    )
}