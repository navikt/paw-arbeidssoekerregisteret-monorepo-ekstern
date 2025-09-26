package no.nav.paw.arbeidssoekerregisteret.repository

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v3.Egenvurdering
import java.time.Instant
import java.util.*

interface EgenvurderingRepository {
    fun finnNyesteProfileringFraÅpenPeriodeUtenEgenvurdering(ident: String): NyesteProfilering?
    fun lagrePeriode(periode: Periode)
    fun lagreProfilering(profilering: Profilering)
    fun lagreEgenvurdering(egenvurdering: Egenvurdering)
    fun slettPeriode(periodeId: UUID): Boolean
    fun finnProfilering(profileringId: UUID, ident: String): ProfileringRow?
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