package no.nav.paw.arbeidssoekerregisteret.repository

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v3.Egenvurdering
import no.nav.paw.felles.model.Identitetsnummer
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.*

object EgenvurderingPostgresRepository : EgenvurderingRepository {

    override fun finnNyesteProfileringFraÅpenPeriodeUtenEgenvurdering(ident: Identitetsnummer): NyesteProfilering? =
        transaction {
            ProfileringTable.join(
                otherTable = PeriodeTable,
                joinType = JoinType.INNER,
                onColumn = ProfileringTable.periodeId,
                otherColumn = PeriodeTable.id
            ).join(
                otherTable = EgenvurderingTable,
                joinType = JoinType.LEFT,
                onColumn = ProfileringTable.id,
                otherColumn = EgenvurderingTable.profileringId
            ).selectAll()
                .where {
                    PeriodeTable.identitetsnummer eq ident.verdi and
                            PeriodeTable.avsluttet.isNull() and
                            EgenvurderingTable.id.isNull()
                }
                .orderBy(ProfileringTable.profileringTidspunkt to SortOrder.DESC)
                .limit(1)
                .firstOrNull()
                ?.let { row ->
                    NyesteProfilering(
                        id = row[ProfileringTable.id],
                        profilertTil = row[ProfileringTable.profilertTil],
                        profileringTidspunkt = row[ProfileringTable.profileringTidspunkt],
                        periodeStartetTidspunkt = row[PeriodeTable.startet]
                    )
                }
        }

    override fun lagreEgenvurdering(egenvurdering: Egenvurdering) {
        EgenvurderingTable.insert {
            it[id] = egenvurdering.id
            it[profileringId] = egenvurdering.profileringId
            it[EgenvurderingTable.egenvurdering] = egenvurdering.egenvurdering.name
        }
    }

    override fun lagrePeriode(periode: Periode) {
        PeriodeTable.insertIgnore {
            it[id] = periode.id
            it[identitetsnummer] = periode.identitetsnummer
            it[startet] = periode.startet.tidspunkt
            it[avsluttet] = periode.avsluttet?.tidspunkt
        }
    }

    override fun lagreProfilering(profilering: Profilering) {
        ProfileringTable.insertIgnore {
            it[id] = profilering.id
            it[periodeId] = profilering.periodeId
            it[profileringTidspunkt] = profilering.sendtInnAv.tidspunkt
            it[profilertTil] = profilering.profilertTil.name
        }
    }

    override fun slettPeriode(periodeId: UUID): Boolean {
        ProfileringTable.deleteWhere { ProfileringTable.periodeId eq periodeId }
        return PeriodeTable.deleteWhere { PeriodeTable.id eq periodeId } > 0
    }

    override fun finnProfilering(profileringId: UUID, ident: Identitetsnummer): ProfileringRow? = transaction {
        ProfileringTable.join(
            otherTable = PeriodeTable,
            joinType = JoinType.INNER,
            onColumn = ProfileringTable.periodeId,
            otherColumn = PeriodeTable.id
        ).selectAll()
            .where { ProfileringTable.id eq profileringId and (PeriodeTable.identitetsnummer eq ident.verdi) }
            .firstOrNull()
            ?.let { row ->
                ProfileringRow(
                    id = row[ProfileringTable.id],
                    periodeId = row[ProfileringTable.periodeId],
                    profilertTil = row[ProfileringTable.profilertTil]
                )
            }
    }
}
