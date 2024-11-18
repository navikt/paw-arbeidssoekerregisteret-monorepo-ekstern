package no.nav.paw.arbeidssoekerregisteret.api.oppslag.database

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Paging
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ProfileringRow
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import java.util.*

object ProfileringFunctions {

    fun findForPeriodeIdList(
        periodeIdList: List<UUID>,
        paging: Paging = Paging()
    ): List<ProfileringRow> {
        return ProfileringTable
            .join(MetadataTable, JoinType.LEFT, ProfileringTable.sendtInnAvId, MetadataTable.id)
            .join(BrukerTable, JoinType.LEFT, MetadataTable.utfoertAvId, BrukerTable.id)
            .join(TidspunktFraKildeTable, JoinType.LEFT, MetadataTable.tidspunktFraKildeId, TidspunktFraKildeTable.id)
            .selectAll()
            .where { ProfileringTable.periodeId inList periodeIdList }
            //.orderBy(MetadataTable.tidspunkt, paging.ordering)
            //.limit(paging.size).offset(paging.offset)
            .map { it.toProfileringRow() }
    }

    fun findForIdentitetsnummerList(
        identitetsnummerList: List<Identitetsnummer>,
        paging: Paging = Paging()
    ): List<ProfileringRow> {
        val identer = identitetsnummerList.map { it.verdi }
        return ProfileringTable
            .join(MetadataTable, JoinType.LEFT, ProfileringTable.sendtInnAvId, MetadataTable.id)
            .join(BrukerTable, JoinType.LEFT, MetadataTable.utfoertAvId, BrukerTable.id)
            .join(TidspunktFraKildeTable, JoinType.LEFT, MetadataTable.tidspunktFraKildeId, TidspunktFraKildeTable.id)
            .join(PeriodeTable, JoinType.LEFT, ProfileringTable.periodeId, PeriodeTable.periodeId)
            .selectAll()
            .where { PeriodeTable.identitetsnummer inList identer }
            //.orderBy(MetadataTable.tidspunkt, paging.ordering)
            //.limit(paging.size).offset(paging.offset)
            .map { it.toProfileringRow() }
    }

    fun hentForPeriodeIdAndOpplysningId(
        periodeId: UUID,
        opplysningId: UUID
    ): ProfileringRow? {
        return ProfileringTable
            .join(MetadataTable, JoinType.LEFT, ProfileringTable.sendtInnAvId, MetadataTable.id)
            .join(BrukerTable, JoinType.LEFT, MetadataTable.utfoertAvId, BrukerTable.id)
            .join(TidspunktFraKildeTable, JoinType.LEFT, MetadataTable.tidspunktFraKildeId, TidspunktFraKildeTable.id)
            .selectAll()
            .where { ProfileringTable.periodeId eq periodeId and (ProfileringTable.opplysningerOmArbeidssoekerId eq opplysningId) }
            .firstOrNull()
            ?.toProfileringRow()
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

