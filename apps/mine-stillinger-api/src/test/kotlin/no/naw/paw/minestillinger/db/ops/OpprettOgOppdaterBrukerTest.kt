package no.naw.paw.minestillinger.db.ops

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.paw.model.asIdentitetsnummer
import no.nav.paw.test.data.periode.MetadataFactory
import no.nav.paw.test.data.periode.PeriodeFactory
import no.naw.paw.minestillinger.db.initDatabase
import no.naw.paw.minestillinger.domain.KanTilbysTjenesten
import no.naw.paw.minestillinger.domain.TjenesteStatus
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Instant

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
                val bruker = hentBrukerProfil(periode.identitetsnummer.asIdentitetsnummer())
                bruker.shouldNotBeNull()
                bruker.identitetsnummer.verdi shouldBe periode.identitetsnummer
                bruker.arbeidssoekerperiodeId shouldBe periode.id
                bruker.kanTilbysTjenesten shouldBe KanTilbysTjenesten.UKJENT
                bruker.harBruktTjenesten shouldBe false
                bruker.tjenestestatus shouldBe TjenesteStatus.INAKTIV
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
            val brukerFraDb = hentBrukerProfil(periodeAvsluttet.identitetsnummer.asIdentitetsnummer())
            brukerFraDb.shouldNotBeNull()
            brukerFraDb.identitetsnummer.verdi shouldBe periodeAvsluttet.identitetsnummer
            brukerFraDb.arbeidssoekerperiodeId shouldBe periodeAvsluttet.id
            brukerFraDb.kanTilbysTjenesten shouldBe KanTilbysTjenesten.UKJENT
            brukerFraDb.arbeidssoekerperiodeAvsluttet shouldBe periodeAvsluttet.avsluttet.tidspunkt
        }
    }

    "Sjekk at vi kan oppdatere kanTilbysTjenesten" {
        transaction {
            opprettOgOppdaterBruker(periode)
        }
        val nå = Instant.now()
        val kanTilbysTjenesten = KanTilbysTjenesten.JA
        transaction {
            val kunneOppdatereBrukerprofil = setKanTilbysTjenesten(
                identitetsnummer = periode.identitetsnummer.asIdentitetsnummer(),
                tidspunkt = nå,
                kanTilbysTjenesten = kanTilbysTjenesten
            )
            kunneOppdatereBrukerprofil shouldBe true
            val brukerFraDb = hentBrukerProfil(periode.identitetsnummer.asIdentitetsnummer())
            brukerFraDb.shouldNotBeNull()
            brukerFraDb.kanTilbysTjenesten shouldBe kanTilbysTjenesten
        }
    }

    "Sjekk at vi kan oppdatere tjenestestatus" {
        transaction {
            opprettOgOppdaterBruker(periode)
        }
        val brukerprofil = hentBrukerProfil(periode.identitetsnummer.asIdentitetsnummer())
        brukerprofil.shouldNotBeNull()
        brukerprofil.tjenestestatus shouldBe TjenesteStatus.INAKTIV

        val tjenestestatus = TjenesteStatus.AKTIV
        transaction {
            val kunneOppdatereBrukerprofil = setTjenestatus(
                identitetsnummer = periode.identitetsnummer.asIdentitetsnummer(),
                nyTjenestestatus = tjenestestatus
            )
            kunneOppdatereBrukerprofil shouldBe true
            val brukerFraDb = hentBrukerProfil(periode.identitetsnummer.asIdentitetsnummer())
            brukerFraDb.shouldNotBeNull()
            brukerFraDb.tjenestestatus shouldBe tjenestestatus
        }
    }
})
