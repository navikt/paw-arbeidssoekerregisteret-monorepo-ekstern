package no.nav.paw.arbeidssoekerregisteret.api.oppslag.database

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ProfileringResponse
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import java.util.*

fun Transaction.finnProfileringer(periodeId: UUID): List<ProfileringResponse> {
    return ProfileringTable
        .join(MetadataTable, JoinType.LEFT, ProfileringTable.sendtInnAvId, MetadataTable.id)
        .join(BrukerTable, JoinType.LEFT, MetadataTable.utfoertAvId, BrukerTable.id)
        .join(TidspunktFraKildeTable, JoinType.LEFT, MetadataTable.tidspunktFraKildeId, TidspunktFraKildeTable.id)
        .selectAll().where { ProfileringTable.periodeId eq periodeId }.map { it.toProfileringResponse() }
}

fun Transaction.finnProfileringer(identitetsnummerList: List<Identitetsnummer>): List<ProfileringResponse> {
    val identer = identitetsnummerList.map { it.verdi }
    return ProfileringTable
        .join(MetadataTable, JoinType.LEFT, ProfileringTable.sendtInnAvId, MetadataTable.id)
        .join(BrukerTable, JoinType.LEFT, MetadataTable.utfoertAvId, BrukerTable.id)
        .join(TidspunktFraKildeTable, JoinType.LEFT, MetadataTable.tidspunktFraKildeId, TidspunktFraKildeTable.id)
        .join(PeriodeTable, JoinType.LEFT, ProfileringTable.periodeId, PeriodeTable.periodeId)
        .selectAll().where { PeriodeTable.identitetsnummer inList identer }.map { it.toProfileringResponse() }
}

fun Transaction.opprettProfilering(profilering: Profilering) {
    ProfileringTable.insert {
        it[profileringId] = profilering.id
        it[periodeId] = profilering.periodeId
        it[opplysningerOmArbeidssoekerId] = profilering.opplysningerOmArbeidssokerId
        it[sendtInnAvId] = opprettMetadata(profilering.sendtInnAv)
        it[profilertTil] = profilering.profilertTil
        it[jobbetSammenhengendeSeksAvTolvSisteManeder] = profilering.jobbetSammenhengendeSeksAvTolvSisteMnd
        it[alder] = profilering.alder
    }
}
