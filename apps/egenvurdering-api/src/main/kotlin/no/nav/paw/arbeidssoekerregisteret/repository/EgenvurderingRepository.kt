package no.nav.paw.arbeidssoekerregisteret.repository

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v3.Egenvurdering
import no.nav.paw.felles.model.Identitetsnummer
import java.time.Instant
import java.util.*

interface EgenvurderingRepository {
    fun finnNyesteProfileringFraÅpenPeriodeUtenEgenvurdering(ident: Identitetsnummer): NyesteProfilering?
    fun lagrePeriode(periode: Periode)
    fun lagreProfilering(profilering: Profilering)
    fun lagreEgenvurdering(egenvurdering: Egenvurdering)
    fun slettPeriode(periodeId: UUID): Boolean
    fun finnProfilering(profileringId: UUID, ident: Identitetsnummer): ProfileringRow?
}

data class NyesteProfilering(
    val id: UUID,
    val profilertTil: String,
    val tidspunkt: Instant,
)

data class ProfileringRow(
    val id: UUID,
    val periodeId: UUID,
    val profilertTil: String,
)