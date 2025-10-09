package no.naw.paw.brukerprofiler.domain

import no.nav.paw.model.Identitetsnummer
import java.time.Instant
import java.util.UUID

data class BrukerProfil(
    val id: Long,
    val identitetsnummer: Identitetsnummer,
    val tjenestenErAktiv: Boolean,
    val harBruktTjenesten: Boolean,
    val arbeidssoekerperiodeId: UUID,
    val arbeidssoekerperiodeAvsluttet: Instant?
)