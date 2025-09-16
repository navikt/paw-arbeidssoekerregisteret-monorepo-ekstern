package no.nav.paw.arbeidssoekerregisteret.repository

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v2.Egenvurdering
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.transaction

object EgenvurderingPostgresRepository : EgenvurderingRepository {
    override fun lagrePerioderOgProfileringer(records: ConsumerRecords<Long, SpecificRecord>) = transaction {
        records.asSequence().forEach { record ->
            when (val value = record.value()) {
                is Periode -> lagrePeriode(value)
                is Profilering -> lagreProfilering(value)
            }
        }
    }

    override fun finnNyesteProfileringFraÅpenPeriodeUtenEgenvurdering(ident: String): NyesteProfilering? = transaction {
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
                PeriodeTable.identitetsnummer eq ident and
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
                    tidspunkt = row[ProfileringTable.profileringTidspunkt]
                )
            }
    }

    private fun lagrePeriode(periode: Periode) {
        PeriodeTable.insert {
            it[id] = periode.id
            it[identitetsnummer] = periode.identitetsnummer
            it[startet] = periode.startet.tidspunkt
            it[avsluttet] = periode.avsluttet?.tidspunkt
        }
    }

    override fun lagreEgenvurdering(egenvurdering: Egenvurdering): InsertStatement<Number> = transaction {
        EgenvurderingTable.insert {
            it[id] = egenvurdering.id
            it[profileringId] = egenvurdering.profileringId
            it[EgenvurderingTable.egenvurdering] = egenvurdering.egenvurdering.name
        }
    }

    private fun lagreProfilering(profilering: Profilering) {
        ProfileringTable.insert {
            it[id] = profilering.id
            it[periodeId] = profilering.periodeId
            it[profileringTidspunkt] = profilering.sendtInnAv.tidspunkt
            it[profilertTil] = profilering.profilertTil.name
        }
    }
}
