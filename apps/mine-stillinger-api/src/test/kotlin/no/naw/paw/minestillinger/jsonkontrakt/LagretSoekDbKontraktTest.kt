package no.naw.paw.minestillinger.jsonkontrakt

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.test.data.periode.PeriodeFactory
import no.naw.paw.minestillinger.db.SoekTable
import no.naw.paw.minestillinger.db.initDatabase
import no.naw.paw.minestillinger.db.ops.databaseConfigFrom
import no.naw.paw.minestillinger.db.ops.hentBrukerProfilUtenFlagg
import no.naw.paw.minestillinger.db.ops.hentSoek
import no.naw.paw.minestillinger.db.ops.lagreSoek
import no.naw.paw.minestillinger.db.ops.opprettOgOppdaterBruker
import no.naw.paw.minestillinger.db.ops.postgreSQLContainer
import no.naw.paw.minestillinger.db.ops.slettAlleSoekForBruker
import no.naw.paw.minestillinger.domain.BrukerId
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * JSON-kontrakt for lagret `soek` gjennom hele databaseveien ([lagreSoek] og [hentSoek]).
 * Krever Docker (Testcontainers/Postgres).
 *
 * Postgres `jsonb` lagrer ikke feltrekkefølge eller mellomrom. Det som leses rått fra
 * [SoekTable] sammenlignes derfor uten krav til feltrekkefølge. Streng feltrekkefølge
 * dekkes av [LagretSoekKontraktTest].
 *
 * Fasitfilene må aldri endres. Et nytt format gir en ny fil; gamle filer skal bli liggende
 * og fortsatt kunne leses av [hentSoek].
 */
class LagretSoekDbKontraktTest : FreeSpec({
    val postgres = postgreSQLContainer()
    val dataSource = autoClose(initDatabase(databaseConfigFrom(postgres)))
    beforeSpec { Database.connect(dataSource) }

    val periode = PeriodeFactory.create().build(avsluttet = null)
    var brukerIdEllerNull: BrukerId? = null
    beforeSpec {
        brukerIdEllerNull = transaction {
            opprettOgOppdaterBruker(periode)
            requireNotNull(hentBrukerProfilUtenFlagg(Identitetsnummer(periode.identitetsnummer))).id
        }
    }
    val brukerId: () -> BrukerId = { requireNotNull(brukerIdEllerNull) }
    beforeEach { transaction { slettAlleSoekForBruker(brukerId()) } }

    lagredeSoekFasiter.forEach { (fil, soek) ->
        "Lagret ${soek.soekType} i $fil" - {
            "lagreSoek skriver innhold og type likt fasitfilen" {
                val (type, raa) = transaction {
                    lagreSoek(brukerId(), Eksempeldata.TIDSPUNKT_1, soek)
                    SoekTable.selectAll()
                        .where { SoekTable.brukerId eq brukerId().verdi }
                        .single()
                        .let { it[SoekTable.type] to it[SoekTable.soek] }
                }
                type shouldBe soek.soekType.name
                raa skalVaereLikFasitUtenFeltrekkefoelge fil
            }
            "hentSoek leser fasitfilen til samme søk" {
                val hentet = transaction {
                    SoekTable.insert {
                        it[SoekTable.brukerId] = brukerId().verdi
                        it[SoekTable.type] = soek.soekType.name
                        it[SoekTable.soek] = JsonKontrakt.lesFasit(fil)
                        it[SoekTable.opprettet] = Eksempeldata.TIDSPUNKT_1
                        it[SoekTable.sistKjoert] = null
                    }
                    hentSoek(brukerId())
                }
                hentet shouldHaveSize 1
                hentet.single().soek shouldBe soek
            }
        }
    }
})
