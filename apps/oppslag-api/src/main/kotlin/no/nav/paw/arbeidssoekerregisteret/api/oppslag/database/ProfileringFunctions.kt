package no.nav.paw.arbeidssoekerregisteret.api.oppslag.database

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ProfileringRow
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import java.util.*

object ProfileringFunctions {

    fun findForPeriodeId(periodeId: UUID): List<ProfileringRow> {
        return ProfileringTable
            .join(MetadataTable, JoinType.LEFT, ProfileringTable.sendtInnAvId, MetadataTable.id)
            .join(BrukerTable, JoinType.LEFT, MetadataTable.utfoertAvId, BrukerTable.id)
            .join(TidspunktFraKildeTable, JoinType.LEFT, MetadataTable.tidspunktFraKildeId, TidspunktFraKildeTable.id)
            .selectAll().where { ProfileringTable.periodeId eq periodeId }.map { it.toProfileringRow() }
    }

    fun findForIdentitetsnummerList(identitetsnummerList: List<Identitetsnummer>): List<ProfileringRow> {
        val identer = identitetsnummerList.map { it.verdi }
        return ProfileringTable
            .join(MetadataTable, JoinType.LEFT, ProfileringTable.sendtInnAvId, MetadataTable.id)
            .join(BrukerTable, JoinType.LEFT, MetadataTable.utfoertAvId, BrukerTable.id)
            .join(TidspunktFraKildeTable, JoinType.LEFT, MetadataTable.tidspunktFraKildeId, TidspunktFraKildeTable.id)
            .join(PeriodeTable, JoinType.LEFT, ProfileringTable.periodeId, PeriodeTable.periodeId)
            .selectAll().where { PeriodeTable.identitetsnummer inList identer }.map { it.toProfileringRow() }
    }

    fun insert(profilering: Profilering) {
        ProfileringTable.insert {
            it[profileringId] = profilering.id
            it[periodeId] = profilering.periodeId
            it[opplysningerOmArbeidssoekerId] = profilering.opplysningerOmArbeidssokerId
            it[sendtInnAvId] = MetadataFunctions.insert(profilering.sendtInnAv)
            it[profilertTil] = profilering.profilertTil
            it[jobbetSammenhengendeSeksAvTolvSisteManeder] = profilering.jobbetSammenhengendeSeksAvTolvSisteMnd
            it[alder] = profilering.alder
        }
    }

}

