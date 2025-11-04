package no.naw.paw.minestillinger.db.ops

import no.naw.paw.minestillinger.brukerprofil.flagg.Flagg
import no.naw.paw.minestillinger.brukerprofil.flagg.HarBeskyttetAdresseFlaggtype
import no.naw.paw.minestillinger.brukerprofil.flagg.LagretFlagg
import no.naw.paw.minestillinger.brukerprofil.flagg.ListeMedFlagg
import no.naw.paw.minestillinger.brukerprofil.flagg.TjenestenErAktivFlaggtype
import no.naw.paw.minestillinger.brukerprofil.flagg.flaggType
import no.naw.paw.minestillinger.db.BrukerFlaggTable
import no.naw.paw.minestillinger.db.BrukerTable
import no.naw.paw.minestillinger.domain.BrukerId
import no.naw.paw.minestillinger.domain.BrukerProfil
import no.naw.paw.minestillinger.domain.BrukerProfilerUtenFlagg
import no.naw.paw.minestillinger.domain.medFlagg
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.batchUpsert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Instant

fun skrivFlaggTilDB(brukerId: BrukerId, listeMedFlagg: Iterable<LagretFlagg>) {
    BrukerFlaggTable.batchUpsert(
        data = listeMedFlagg,
        keys = arrayOf(BrukerFlaggTable.brukerId, BrukerFlaggTable.navn),
        onUpdateExclude = listOf(BrukerFlaggTable.brukerId, BrukerFlaggTable.navn)
    ) { row ->
        this[BrukerFlaggTable.brukerId] = brukerId.verdi
        this[BrukerFlaggTable.navn] = row.type.type
        this[BrukerFlaggTable.verdi] = row.verdi
        this[BrukerFlaggTable.tidspunkt] = row.tidspunkt
    }
}

fun lesFlaggFraDB(brukerId: BrukerId): List<Flagg> {
    return BrukerFlaggTable.selectAll()
        .where { BrukerFlaggTable.brukerId eq brukerId.verdi }
        .map { row ->
            val lagretFlagNavn: String = row[BrukerFlaggTable.navn]
            flaggType(lagretFlagNavn)
                ?.flagg(verdi = row[BrukerFlaggTable.verdi], tidspunkt = row[BrukerFlaggTable.tidspunkt])
                ?: throw IllegalStateException("Ukjent flagg lagret i databasen: $lagretFlagNavn")

        }
}

fun hentAlleAktiveBrukereMedUtløptAdressebeskyttelseFlagg(
    alleFraFørDetteErUtløpt: Instant
): List<BrukerProfil> {
    return transaction {
        val aktivFlagg = BrukerFlaggTable.alias("aktiv_flagg")
        val adresseFlagg = BrukerFlaggTable.alias("adresse_flagg")

        BrukerTable
            .innerJoin(
                otherTable = aktivFlagg,
                onColumn = { BrukerTable.id },
                otherColumn = { aktivFlagg[BrukerFlaggTable.brukerId] })
            .innerJoin(
                otherTable = adresseFlagg,
                onColumn = { BrukerTable.id },
                otherColumn = { adresseFlagg[BrukerFlaggTable.brukerId] })
            .selectAll()
            .forUpdate()
            .where {
                (aktivFlagg[BrukerFlaggTable.navn] eq TjenestenErAktivFlaggtype.type) and
                (aktivFlagg[BrukerFlaggTable.verdi] eq true) and
                (adresseFlagg[BrukerFlaggTable.navn] eq HarBeskyttetAdresseFlaggtype.type) and
                (adresseFlagg[BrukerFlaggTable.tidspunkt] less alleFraFørDetteErUtløpt)
            }
            .map { row ->
                brukerprofilUtenFlagg(row).medFlagg(
                    ListeMedFlagg.listeMedFlagg(lesFlaggFraDB(BrukerId(row[BrukerTable.id])))
                )
            }
    }
}
