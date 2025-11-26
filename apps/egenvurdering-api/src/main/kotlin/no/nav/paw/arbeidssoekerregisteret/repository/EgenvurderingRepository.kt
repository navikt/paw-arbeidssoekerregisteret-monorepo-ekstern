package no.nav.paw.arbeidssoekerregisteret.repository

import no.nav.paw.arbeidssoekerregisteret.model.EgenvurderingRow
import no.nav.paw.arbeidssoekerregisteret.model.NyesteProfileringRow
import no.nav.paw.arbeidssoekerregisteret.model.ProfileringRow
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v3.Egenvurdering
import no.nav.paw.felles.model.Identitetsnummer
import java.util.*

interface EgenvurderingRepository {
    fun finnNyesteProfilering(identitetsnummer: Identitetsnummer): NyesteProfileringRow?
    fun lagrePeriode(periode: Periode)
    fun lagreProfilering(profilering: Profilering)
    fun lagreEgenvurdering(egenvurdering: Egenvurdering)
    fun slettPeriode(periodeId: UUID): Boolean
    fun finnProfilering(profileringId: UUID, identitetsnummer: Identitetsnummer): ProfileringRow?
    fun finnEgenvurderinger(identitetsnummer: Identitetsnummer): List<EgenvurderingRow>
}