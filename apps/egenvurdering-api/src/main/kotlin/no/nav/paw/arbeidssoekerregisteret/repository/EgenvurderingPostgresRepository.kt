package no.nav.paw.arbeidssoekerregisteret.repository

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v2.Egenvurdering
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.statements.InsertStatement
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object EgenvurderingPostgresRepository : EgenvurderingRepository {
    override fun lagrePerioderOgProfileringer(records: ConsumerRecords<Long, SpecificRecord>) = transaction {
        records.asSequence().forEach { record ->
            when (val value = record.value()) {
                is Periode -> lagreEllerOppdaterPeriode(value)
                is Profilering -> lagreProfilering(value)
            }
        }
    }

    override fun lagreEgenvurdering(egenvurdering: Egenvurdering): InsertStatement<Number> = transaction {
        EgenvurderingTable.insert {
            it[id] = egenvurdering.id
            it[profileringId] = egenvurdering.profileringId
            it[EgenvurderingTable.egenvurdering] = egenvurdering.egenvurdering.name
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

    private fun lagreEllerOppdaterPeriode(periode: Periode) {
        //language=SQL
        val upsertStatement = """
            INSERT INTO ${PeriodeTable.tableName} (id, identitetsnummer, startet, avsluttet)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE
                SET avsluttet = EXCLUDED.avsluttet
            WHERE ${PeriodeTable.tableName}.avsluttet IS NULL
              AND EXCLUDED.avsluttet IS NOT NULL
        """.trimIndent()

        val args = listOf(
            PeriodeTable.id.columnType to periode.id,
            PeriodeTable.identitetsnummer.columnType to periode.identitetsnummer,
            PeriodeTable.startet.columnType to periode.startet.tidspunkt,
            PeriodeTable.avsluttet.columnType to periode.avsluttet?.tidspunkt
        )

        TransactionManager.current().exec(upsertStatement, args)
    }

    private fun lagreProfilering(profilering: Profilering) {
        ProfileringTable.insertIgnore {
            it[id] = profilering.id
            it[periodeId] = profilering.periodeId
            it[profileringTidspunkt] = profilering.sendtInnAv.tidspunkt
            it[profilertTil] = profilering.profilertTil.name
        }
    }
}
