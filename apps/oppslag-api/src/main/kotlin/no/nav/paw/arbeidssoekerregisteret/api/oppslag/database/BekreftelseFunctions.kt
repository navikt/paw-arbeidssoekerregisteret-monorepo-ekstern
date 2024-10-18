package no.nav.paw.arbeidssoekerregisteret.api.oppslag.database

import org.jetbrains.exposed.sql.Transaction
import java.util.*
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.BekreftelseResponse
import no.nav.paw.bekreftelse.melding.v1.vo.Bruker
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.bekreftelse.melding.v1.vo.Metadata
import no.nav.paw.bekreftelse.melding.v1.vo.Svar
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.upsert

fun Transaction.finnBekreftelserForPeriodeId(periodeId: UUID): List<BekreftelseResponse> {
    return BekreftelseTable
        .join(BekreftelseSvarTable, JoinType.LEFT, BekreftelseTable.svarId, BekreftelseSvarTable.id)
        .join(MetadataTable, JoinType.LEFT, BekreftelseSvarTable.sendtInnId, MetadataTable.id)
        .join(BrukerTable, JoinType.LEFT, MetadataTable.utfoertAvId, BrukerTable.id)
        .selectAll()
        .where { BekreftelseTable.periodeId eq periodeId }.map { it.toBekreftelseResponse() }
}

fun Transaction.hentBekreftelseForId(bekreftelseMeldingId: UUID): BekreftelseResponse? {
    return BekreftelseTable
        .join(BekreftelseSvarTable, JoinType.LEFT, BekreftelseTable.svarId, BekreftelseSvarTable.id)
        .join(MetadataTable, JoinType.LEFT, BekreftelseSvarTable.sendtInnId, MetadataTable.id)
        .join(BrukerTable, JoinType.LEFT, MetadataTable.utfoertAvId, BrukerTable.id)
        .selectAll()
        .where { BekreftelseTable.bekreftelseMeldingId eq bekreftelseMeldingId }.singleOrNull()?.toBekreftelseResponse()
}

fun Transaction.opprettBekreftelse(bekreftelse: Bekreftelse) {
    BekreftelseTable.insert {
        it[periodeId] = bekreftelse.periodeId
        it[namespace] = bekreftelse.namespace
        it[bekreftelseMeldingId] = bekreftelse.id
        it[svarId] = opprettBekreftelseSvar(bekreftelse.svar)
    }
}

fun Transaction.opprettBekreftelseSvar(svar: Svar) =
    BekreftelseSvarTable.insertAndGetId {
        it[sendtInnId] = opprettSvarMetadata(svar.sendtInn)
        it[gjelderFra] = svar.gjelderFra
        it[gjelderTil] = svar.gjelderTil
        it[harJobbetIDennePerioden] = svar.harJobbetIDennePerioden
        it[vilFortsetteSomArbeidssoeker] = svar.vilFortsetteSomArbeidssoeker
    }.value

fun Transaction.opprettSvarMetadata(metadata: Metadata): Long =
    MetadataTable.insertAndGetId {
        it[utfoertAvId] = opprettEllerOppdaterBruker(metadata.utfoertAv)
        it[tidspunkt] = metadata.tidspunkt
        it[kilde] = metadata.kilde
        it[aarsak] = metadata.aarsak
    }.value

private fun Transaction.opprettEllerOppdaterBruker(bruker: Bruker): Long {
    val result = BekreftelseBrukerTable.upsert(
        BekreftelseBrukerTable.type,
        BekreftelseBrukerTable.brukerId,
        where = { (BekreftelseBrukerTable.type eq bruker.type) and (BekreftelseBrukerTable.brukerId eq bruker.id) }
    ) {
        it[type] = bruker.type
        it[brukerId] = bruker.id
    }.resultedValues?.singleOrNull() ?: throw IllegalStateException("Upsert-operasjon returnerte ingen resultat")
    return result[BekreftelseBrukerTable.id].value
}