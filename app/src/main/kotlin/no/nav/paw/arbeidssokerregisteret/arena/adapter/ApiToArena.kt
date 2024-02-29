package no.nav.paw.arbeidssokerregisteret.arena.adapter

import no.nav.paw.arbeidssokerregisteret.api.v1.*
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v4.Utdanning

fun ProfilertTil.toArena(): no.nav.paw.arbeidssokerregisteret.arena.v1.ProfilertTil =
    when (this) {
        ProfilertTil.UKJENT_VERDI -> no.nav.paw.arbeidssokerregisteret.arena.v1.ProfilertTil.UKJENT_VERDI
        ProfilertTil.UDEFINERT -> no.nav.paw.arbeidssokerregisteret.arena.v1.ProfilertTil.UDEFINERT
        ProfilertTil.ANTATT_GODE_MULIGHETER -> no.nav.paw.arbeidssokerregisteret.arena.v1.ProfilertTil.ANTATT_GODE_MULIGHETER
        ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING -> no.nav.paw.arbeidssokerregisteret.arena.v1.ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
        ProfilertTil.OPPGITT_HINDRINGER -> no.nav.paw.arbeidssokerregisteret.arena.v1.ProfilertTil.OPPGITT_HINDRINGER
    }

fun Jobbsituasjon.toArena(): no.nav.paw.arbeidssokerregisteret.arena.v1.Jobbsituasjon =
    no.nav.paw.arbeidssokerregisteret.arena.v1.Jobbsituasjon(
        this.beskrivelser.map { it.toArena() }
    )

fun BeskrivelseMedDetaljer.toArena(): no.nav.paw.arbeidssokerregisteret.arena.v1.BeskrivelseMedDetaljer =
    no.nav.paw.arbeidssokerregisteret.arena.v1.BeskrivelseMedDetaljer(
        beskrivelse.toArena(),
        detaljer
    )

fun Beskrivelse.toArena(): no.nav.paw.arbeidssokerregisteret.arena.v1.Beskrivelse =
    when (this) {
        Beskrivelse.UKJENT_VERDI -> no.nav.paw.arbeidssokerregisteret.arena.v1.Beskrivelse.UKJENT_VERDI
        Beskrivelse.UDEFINERT -> no.nav.paw.arbeidssokerregisteret.arena.v1.Beskrivelse.UDEFINERT
        Beskrivelse.HAR_SAGT_OPP -> no.nav.paw.arbeidssokerregisteret.arena.v1.Beskrivelse.HAR_SAGT_OPP
        Beskrivelse.HAR_BLITT_SAGT_OPP -> no.nav.paw.arbeidssokerregisteret.arena.v1.Beskrivelse.HAR_BLITT_SAGT_OPP
        Beskrivelse.ER_PERMITTERT -> no.nav.paw.arbeidssokerregisteret.arena.v1.Beskrivelse.ER_PERMITTERT
        Beskrivelse.ALDRI_HATT_JOBB -> no.nav.paw.arbeidssokerregisteret.arena.v1.Beskrivelse.ALDRI_HATT_JOBB
        Beskrivelse.IKKE_VAERT_I_JOBB_SISTE_2_AAR -> no.nav.paw.arbeidssokerregisteret.arena.v1.Beskrivelse.IKKE_VAERT_I_JOBB_SISTE_2_AAR
        Beskrivelse.AKKURAT_FULLFORT_UTDANNING -> no.nav.paw.arbeidssokerregisteret.arena.v1.Beskrivelse.AKKURAT_FULLFORT_UTDANNING
        Beskrivelse.VIL_BYTTE_JOBB -> no.nav.paw.arbeidssokerregisteret.arena.v1.Beskrivelse.VIL_BYTTE_JOBB
        Beskrivelse.USIKKER_JOBBSITUASJON -> no.nav.paw.arbeidssokerregisteret.arena.v1.Beskrivelse.USIKKER_JOBBSITUASJON
        Beskrivelse.MIDLERTIDIG_JOBB -> no.nav.paw.arbeidssokerregisteret.arena.v1.Beskrivelse.MIDLERTIDIG_JOBB
        Beskrivelse.DELTIDSJOBB_VIL_MER -> no.nav.paw.arbeidssokerregisteret.arena.v1.Beskrivelse.DELTIDSJOBB_VIL_MER
        Beskrivelse.NY_JOBB -> no.nav.paw.arbeidssokerregisteret.arena.v1.Beskrivelse.NY_JOBB
        Beskrivelse.KONKURS -> no.nav.paw.arbeidssokerregisteret.arena.v1.Beskrivelse.KONKURS
        Beskrivelse.ANNET -> no.nav.paw.arbeidssokerregisteret.arena.v1.Beskrivelse.ANNET
    }

fun Utdanning.toArena(): no.nav.paw.arbeidssokerregisteret.arena.v4.Utdanning =
    no.nav.paw.arbeidssokerregisteret.arena.v4.Utdanning(
        nus,
        bestaatt.toArena(),
        godkjent.toArena(),
    )

fun JaNeiVetIkke.toArena(): no.nav.paw.arbeidssokerregisteret.arena.v1.JaNeiVetIkke =
    when (this) {
        JaNeiVetIkke.JA -> no.nav.paw.arbeidssokerregisteret.arena.v1.JaNeiVetIkke.JA
        JaNeiVetIkke.NEI -> no.nav.paw.arbeidssokerregisteret.arena.v1.JaNeiVetIkke.NEI
        JaNeiVetIkke.VET_IKKE -> no.nav.paw.arbeidssokerregisteret.arena.v1.JaNeiVetIkke.VET_IKKE
    }

fun Metadata.toArena(): no.nav.paw.arbeidssokerregisteret.arena.v1.Metadata =
    no.nav.paw.arbeidssokerregisteret.arena.v1.Metadata(
        tidspunkt,
        utfoertAv.toArena(),
        kilde,
        aarsak
    )

fun Bruker.toArena(): no.nav.paw.arbeidssokerregisteret.arena.v1.Bruker =
    no.nav.paw.arbeidssokerregisteret.arena.v1.Bruker(
        type.toArena(),
        id
    )

fun BrukerType.toArena(): no.nav.paw.arbeidssokerregisteret.arena.v1.BrukerType =
    when (this) {
        BrukerType.SYSTEM -> no.nav.paw.arbeidssokerregisteret.arena.v1.BrukerType.SYSTEM
        BrukerType.VEILEDER -> no.nav.paw.arbeidssokerregisteret.arena.v1.BrukerType.VEILEDER
        BrukerType.SLUTTBRUKER -> no.nav.paw.arbeidssokerregisteret.arena.v1.BrukerType.SLUTTBRUKER
        BrukerType.UKJENT_VERDI -> no.nav.paw.arbeidssokerregisteret.arena.v1.BrukerType.UKJENT_VERDI
        BrukerType.UDEFINERT -> no.nav.paw.arbeidssokerregisteret.arena.v1.BrukerType.UDEFINERT
    }