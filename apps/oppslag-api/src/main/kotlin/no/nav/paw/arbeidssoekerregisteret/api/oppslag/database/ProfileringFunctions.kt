package no.nav.paw.arbeidssoekerregisteret.api.oppslag.database

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Paging
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ProfileringRow
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.felles.model.Identitetsnummer
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.util.*

object ProfileringFunctions {

    fun findForPeriodeIdList(
        periodeIdList: Collection<UUID>,
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
        identitetsnummerList: Collection<Identitetsnummer>,
        paging: Paging = Paging()
    ): List<ProfileringRow> {
        val identer = identitetsnummerList.map { it.value }
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

    fun getForProfileringId(
        profileringId: UUID
    ): ProfileringRow? {
        return ProfileringTable
            .join(MetadataTable, JoinType.LEFT, ProfileringTable.sendtInnAvId, MetadataTable.id)
            .join(BrukerTable, JoinType.LEFT, MetadataTable.utfoertAvId, BrukerTable.id)
            .join(TidspunktFraKildeTable, JoinType.LEFT, MetadataTable.tidspunktFraKildeId, TidspunktFraKildeTable.id)
            .selectAll()
            .where { ProfileringTable.profileringId eq profileringId }
            .firstOrNull()
            ?.toProfileringRow()
    }

    fun getForPeriodeIdAndOpplysningId(
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

