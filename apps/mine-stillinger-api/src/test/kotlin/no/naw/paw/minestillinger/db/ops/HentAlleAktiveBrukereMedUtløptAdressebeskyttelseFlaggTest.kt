package no.naw.paw.minestillinger.db.ops

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import no.nav.paw.test.data.periode.PeriodeFactory
import no.naw.paw.minestillinger.brukerprofil.flagg.HarBeskyttetadresseFlagg
import no.naw.paw.minestillinger.brukerprofil.flagg.TjenestenErAktivFlagg
import no.naw.paw.minestillinger.db.initDatabase
import no.naw.paw.minestillinger.domain.BrukerProfil
import no.naw.paw.minestillinger.domain.BrukerProfilerUtenFlagg
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Duration
import java.time.Instant
import java.util.Collections.synchronizedList
import kotlin.random.Random
import kotlin.random.nextLong

class HentAlleAktiveBrukereMedUtløptAdressebeskyttelseFlaggTest : FreeSpec({
    val postgres = postgreSQLContainer()
    val databaseConfig = databaseConfigFrom(postgres)
    val dataSource = autoClose(initDatabase(databaseConfig))
    beforeSpec {
        Database.connect(dataSource)
    }

    "Vi kan hente alle aktive brukere med utløpt adressebeskyttelse-flagg" - {
        val periodeFactory = PeriodeFactory.create()
        val ola = periodeFactory.build(identitetsnummer = "02345678909")
        val kari = periodeFactory.build(identitetsnummer = "12345678901")
        val rolf = periodeFactory.build(identitetsnummer = "22345678901")
        val turid = periodeFactory.build(identitetsnummer = "32345678901")
        val tidspunkt = Instant.now()
        transaction {
            val olaId = opprettOgOppdaterBruker(ola)
            val kariId = opprettOgOppdaterBruker(kari)
            val rolfId = opprettOgOppdaterBruker(rolf)
            val turidId = opprettOgOppdaterBruker(turid)
            skrivFlaggTilDB(
                brukerId = olaId,
                listeMedFlagg = listOf(
                    HarBeskyttetadresseFlagg(false, tidspunkt - Duration.ofHours(12)),
                    TjenestenErAktivFlagg(true, tidspunkt - Duration.ofDays(120))
                )
            )
            skrivFlaggTilDB(
                brukerId = kariId,
                listeMedFlagg = listOf(
                    HarBeskyttetadresseFlagg(true, tidspunkt - Duration.ofHours(251)),
                    TjenestenErAktivFlagg(false, tidspunkt - Duration.ofDays(120))
                )
            )
            skrivFlaggTilDB(
                brukerId = rolfId,
                listeMedFlagg = listOf(
                    HarBeskyttetadresseFlagg(false, tidspunkt - Duration.ofHours(25)),
                    TjenestenErAktivFlagg(true, tidspunkt - Duration.ofDays(120))
                )
            )
            skrivFlaggTilDB(
                brukerId = turidId,
                listeMedFlagg = listOf(
                    HarBeskyttetadresseFlagg(false, tidspunkt - Duration.ofHours(60)),
                    TjenestenErAktivFlagg(true, tidspunkt - Duration.ofDays(120))
                )
            )
        }

        "Rolf og Turi skal returneres som aktive brukere med utløpt adressebeskyttelse-flagg" {
            val brukereReturnert = synchronizedList(mutableListOf<BrukerProfil>())
            fun selectTråd(): Thread = Thread {
                transaction {
                    val brukere = hentAlleAktiveBrukereMedUtløptAdressebeskyttelseFlagg(
                        alleFraFørDetteErUtløpt = tidspunkt - Duration.ofHours(24)
                    )
                    val sleepTime = Random.nextLong(LongRange(500, 1000))
                    println("Tråd sover i $sleepTime ms")
                    Thread.sleep(sleepTime) //Vi venter litt slit at vi får verifisert at select for update låser radene
                    brukere.forEach { bruker ->
                        skrivFlaggTilDB(
                            brukerId = bruker.id,
                            listeMedFlagg = listOf(
                                HarBeskyttetadresseFlagg(true, tidspunkt)
                            )
                        )
                    }
                    brukereReturnert.addAll(brukere)
                }
            }
            val antallTråder = 16
            (0..antallTråder).map {
                selectTråd().apply { start() }
            }.forEach { it.join() }
            val resultat = brukereReturnert.toList().map { it.identitetsnummer.verdi }
            resultat.size shouldBe 2
            resultat shouldContainExactlyInAnyOrder listOf(
                rolf.identitetsnummer,
                turid.identitetsnummer
            )
        }
    }
})


