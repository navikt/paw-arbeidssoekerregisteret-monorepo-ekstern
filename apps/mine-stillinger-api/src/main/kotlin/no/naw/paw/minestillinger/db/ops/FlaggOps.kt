package no.naw.paw.minestillinger.db.ops

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.naw.paw.minestillinger.brukerprofil.flagg.Flagg
import no.naw.paw.minestillinger.brukerprofil.flagg.Flaggtype
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
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.countDistinct
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.min
import org.jetbrains.exposed.v1.jdbc.batchUpsert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Instant

data class AdressebeskyttelseCacheStatus(
    val eldsteOppdatering: Instant?,
    val manglerFlagg: Boolean
)

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
        .map(ResultRow::tilFlagg)
}

@WithSpan("vedlikehold_hent_adressebeskyttelse_cache_status")
fun hentAdressebeskyttelseCacheStatus(): AdressebeskyttelseCacheStatus {
    return transaction {
        val aktivFlagg = BrukerFlaggTable.alias("aktiv_flagg")
        val adresseFlagg = BrukerFlaggTable.alias("adresse_flagg")
        val eldsteOppdatering = adresseFlagg[BrukerFlaggTable.tidspunkt].min()
        val antallAktiveBrukere = BrukerTable.id.countDistinct()
        val antallBrukereMedAdresseFlagg = adresseFlagg[BrukerFlaggTable.brukerId].countDistinct()

        val row = BrukerTable
            .innerJoin(
                otherTable = aktivFlagg,
                onColumn = { BrukerTable.id },
                otherColumn = { aktivFlagg[BrukerFlaggTable.brukerId] }
            )
            .join(
                joinType = JoinType.LEFT,
                otherTable = adresseFlagg,
                onColumn = BrukerTable.id,
                otherColumn = adresseFlagg[BrukerFlaggTable.brukerId],
                additionalConstraint = {
                    adresseFlagg[BrukerFlaggTable.navn] eq HarBeskyttetAdresseFlaggtype.type
                }
            )
            .select(
                eldsteOppdatering,
                antallAktiveBrukere,
                antallBrukereMedAdresseFlagg
            )
            .where {
                (aktivFlagg[BrukerFlaggTable.navn] eq TjenestenErAktivFlaggtype.type) and
                    (aktivFlagg[BrukerFlaggTable.verdi] eq true)
            }
            .single()

        AdressebeskyttelseCacheStatus(
            eldsteOppdatering = row[eldsteOppdatering],
            manglerFlagg = row[antallBrukereMedAdresseFlagg] < row[antallAktiveBrukere]
        )
    }
}

@WithSpan("vedlikehold_hent_aktive_brukere_med_utløpt_adressebeskyttelse_flagg")
fun hentAlleAktiveBrukereMedUtløptAdressebeskyttelseFlagg(
    alleFraFørDetteErUtløpt: Instant,
    etterBrukerId: BrukerId? = null,
    limit: Int = Int.MAX_VALUE
): List<BrukerProfil> {
    require(limit > 0) { "Limit må være større enn null" }
    return transaction {
        val aktivFlagg = BrukerFlaggTable.alias("aktiv_flagg")
        val adresseFlagg = BrukerFlaggTable.alias("adresse_flagg")

        val brukerprofiler = BrukerTable
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
                val grunnvilkår =
                (aktivFlagg[BrukerFlaggTable.navn] eq TjenestenErAktivFlaggtype.type) and
                (aktivFlagg[BrukerFlaggTable.verdi] eq true) and
                (adresseFlagg[BrukerFlaggTable.navn] eq HarBeskyttetAdresseFlaggtype.type) and
                (adresseFlagg[BrukerFlaggTable.tidspunkt] less alleFraFørDetteErUtløpt)
                etterBrukerId
                ?.let { grunnvilkår and (BrukerTable.id greater it.verdi) }
                ?: grunnvilkår
            }
            .orderBy(BrukerTable.id to SortOrder.ASC)
            .limit(limit)
            .map(::brukerprofilUtenFlagg)

        val flaggPerBruker = lesFlaggFraDB(brukerprofiler.map(BrukerProfilerUtenFlagg::id))
        brukerprofiler.map { brukerprofil ->
            brukerprofil.medFlagg(
                ListeMedFlagg.listeMedFlagg(flaggPerBruker[brukerprofil.id].orEmpty())
            )
        }
    }
}

private fun lesFlaggFraDB(brukerIds: List<BrukerId>): Map<BrukerId, List<Flagg>> {
    if (brukerIds.isEmpty()) return emptyMap()
    return BrukerFlaggTable
        .selectAll()
        .where { BrukerFlaggTable.brukerId inList brukerIds.map(BrukerId::verdi) }
        .map { row ->
            BrukerId(row[BrukerFlaggTable.brukerId]) to row.tilFlagg()
        }
        .groupBy(
            keySelector = Pair<BrukerId, Flagg>::first,
            valueTransform = Pair<BrukerId, Flagg>::second
        )
}

private fun ResultRow.tilFlagg(): Flagg {
    val lagretFlagNavn = this[BrukerFlaggTable.navn]
    return flaggType(lagretFlagNavn)
        ?.flagg(
            verdi = this[BrukerFlaggTable.verdi],
            tidspunkt = this[BrukerFlaggTable.tidspunkt]
        )
        ?: throw IllegalStateException("Ukjent flagg lagret i databasen: $lagretFlagNavn")
}

fun <T: Flagg> hentAlleAktiveBrukereMedUtdatertFlagg(
    alleFraFørDetteErUtløpt: Instant,
    flaggtype: Flaggtype<T>,
    limit: Int
): List<BrukerProfil> {
    return transaction {
        val aktivFlagg = BrukerFlaggTable.alias("aktiv_flagg")
        val flagg = BrukerFlaggTable.alias("flagg_${flaggtype.type}")

        BrukerTable
            .innerJoin(
                otherTable = aktivFlagg,
                onColumn = { BrukerTable.id },
                otherColumn = { aktivFlagg[BrukerFlaggTable.brukerId] })
            .innerJoin(
                otherTable = flagg,
                onColumn = { BrukerTable.id },
                otherColumn = { flagg[BrukerFlaggTable.brukerId] })
            .selectAll()
            .forUpdate()
            .where {
                (aktivFlagg[BrukerFlaggTable.navn] eq TjenestenErAktivFlaggtype.type) and
                        (aktivFlagg[BrukerFlaggTable.verdi] eq true) and
                        (flagg[BrukerFlaggTable.navn] eq flaggtype.type) and
                        (flagg[BrukerFlaggTable.tidspunkt] less alleFraFørDetteErUtløpt)
            }.limit(limit)
            .map { row ->
                brukerprofilUtenFlagg(row).medFlagg(
                    ListeMedFlagg.listeMedFlagg(lesFlaggFraDB(BrukerId(row[BrukerTable.id])))
                )
            }
    }
}

fun <T: Flagg> hentAlleAktiveBrukereSomManglerFlagg(
    flaggtype: Flaggtype<T>,
    limit: Int
): List<BrukerProfil> {
    return transaction {
        val aktivFlagg = BrukerFlaggTable.alias("aktiv_flagg")
        val flagg = BrukerFlaggTable.alias("flagg_${flaggtype.type}")
        BrukerTable
            .innerJoin(
                otherTable = aktivFlagg,
                onColumn = { BrukerTable.id },
                otherColumn = { aktivFlagg[BrukerFlaggTable.brukerId] })
            .join(
                joinType = JoinType.LEFT,
                otherTable = flagg,
                onColumn = BrukerTable.id,
                otherColumn = flagg[BrukerFlaggTable.brukerId],
                additionalConstraint = { flagg[BrukerFlaggTable.navn] eq flaggtype.type }
            )
            .selectAll()
            .where {
                (aktivFlagg[BrukerFlaggTable.navn] eq TjenestenErAktivFlaggtype.type) and
                        (aktivFlagg[BrukerFlaggTable.verdi] eq true) and
                        flagg[BrukerFlaggTable.navn].isNull()
            }.limit(limit)
            .map { row ->
                brukerprofilUtenFlagg(row).medFlagg(
                    ListeMedFlagg.listeMedFlagg(lesFlaggFraDB(BrukerId(row[BrukerTable.id])))
                )
            }
    }
}
