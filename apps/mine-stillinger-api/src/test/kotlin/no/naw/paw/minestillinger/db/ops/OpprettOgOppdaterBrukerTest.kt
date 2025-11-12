package no.naw.paw.minestillinger.db.ops

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.paw.felles.model.asIdentitetsnummer
import no.nav.paw.test.data.periode.MetadataFactory
import no.nav.paw.test.data.periode.PeriodeFactory
import no.naw.paw.minestillinger.db.initDatabase
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class OpprettOgOppdaterBrukerTest : FreeSpec({
    val postgres = postgreSQLContainer()
    val databaseConfig = databaseConfigFrom(postgres)
    val dataSource = autoClose(initDatabase(databaseConfig))
    val periodeFactory = PeriodeFactory.create()
    val metadataFactory = MetadataFactory.create()
    beforeSpec { Database.connect(dataSource) }

    val periode = periodeFactory.build(avsluttet = null)
    "Verifiser oppretting og oppdatering av bruker i databasen" - {

        "Sjekk oppretting av bruker" - {
            "Vi kan lagre en ny bruker uten feil" {
                transaction {
                    opprettOgOppdaterBruker(periode)
                }
            }
            "Vi kan lagre samme bruker på nytt uten feil" {
                transaction {
                    opprettOgOppdaterBruker(periode)
                }
            }
            "Vi kan lese bruker fra databasen" {
                val bruker = hentBrukerProfilUtenFlagg(periode.identitetsnummer.asIdentitetsnummer())
                bruker.shouldNotBeNull()
                bruker.identitetsnummer.value shouldBe periode.identitetsnummer
                bruker.arbeidssoekerperiodeId.verdi shouldBe periode.id
                bruker.arbeidssoekerperiodeAvsluttet.shouldBeNull()
            }
        }
    }
    "Sjekk avslutning av periode" - {
        val periodeAvsluttet = periodeFactory.build(
            id = periode.id,
            identitetsnummer = periode.identitetsnummer,
            avsluttet = metadataFactory.build()
        )
        transaction {
            opprettOgOppdaterBruker(periodeAvsluttet)
            val brukerFraDb = hentBrukerProfilUtenFlagg(periodeAvsluttet.identitetsnummer.asIdentitetsnummer())
            brukerFraDb.shouldNotBeNull()
            brukerFraDb.identitetsnummer.value shouldBe periodeAvsluttet.identitetsnummer
            brukerFraDb.arbeidssoekerperiodeId.verdi shouldBe periodeAvsluttet.id
            brukerFraDb.arbeidssoekerperiodeAvsluttet shouldBe periodeAvsluttet.avsluttet.tidspunkt
        }
    }
})
