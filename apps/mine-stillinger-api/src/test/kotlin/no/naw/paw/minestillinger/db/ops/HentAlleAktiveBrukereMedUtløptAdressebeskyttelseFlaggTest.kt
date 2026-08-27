package no.naw.paw.minestillinger.db.ops

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.paw.test.data.periode.PeriodeFactory
import no.naw.paw.minestillinger.brukerprofil.flagg.HarBeskyttetAdresseFlaggtype
import no.naw.paw.minestillinger.brukerprofil.flagg.HarBeskyttetadresseFlagg
import no.naw.paw.minestillinger.brukerprofil.flagg.TjenestenErAktivFlagg
import no.naw.paw.minestillinger.db.initDatabase
import no.naw.paw.minestillinger.domain.BrukerProfil
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import no.naw.paw.minestillinger.db.BrukerFlaggTable
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Collections.synchronizedList
import java.util.concurrent.CountDownLatch

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
        val tidspunkt = Instant.now().truncatedTo(ChronoUnit.MILLIS)
        var olaBrukerId = -1L
        transaction {
            val olaId = opprettOgOppdaterBruker(ola)
            olaBrukerId = olaId.verdi
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

        "Utløpte adressebeskyttelsesflagg kan hentes i stabile batcher" {
            val førsteBatch = hentAlleAktiveBrukereMedUtløptAdressebeskyttelseFlagg(
                alleFraFørDetteErUtløpt = tidspunkt - Duration.ofHours(24),
                limit = 1
            )
            val andreBatch = hentAlleAktiveBrukereMedUtløptAdressebeskyttelseFlagg(
                alleFraFørDetteErUtløpt = tidspunkt - Duration.ofHours(24),
                etterBrukerId = førsteBatch.single().id,
                limit = 1
            )

            (førsteBatch + andreBatch) shouldHaveSize 2
            (førsteBatch + andreBatch)
                .map { it.identitetsnummer.value } shouldContainExactlyInAnyOrder listOf(
                rolf.identitetsnummer,
                turid.identitetsnummer
            )
        }

        "Rolf og Turi skal returneres som aktive brukere med utløpt adressebeskyttelse-flagg" {
            val brukereReturnert = synchronizedList(mutableListOf<BrukerProfil>())
            val feil = synchronizedList(mutableListOf<Throwable>())
            val startSignal = CountDownLatch(1)
            fun selectTråd(): Thread = Thread {
                try {
                    startSignal.await()
                    transaction {
                        val brukere = hentAlleAktiveBrukereMedUtløptAdressebeskyttelseFlagg(
                            alleFraFørDetteErUtløpt = tidspunkt - Duration.ofHours(24)
                        )
                        Thread.sleep(100)
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
                } catch (error: Throwable) {
                    feil.add(error)
                }
            }
            val antallTråder = 16
            val tråder = (0..antallTråder).map {
                selectTråd().apply { start() }
            }
            startSignal.countDown()
            tråder.forEach { it.join() }

            feil shouldBe emptyList()
            val resultat = brukereReturnert.toList().map { it.identitetsnummer.value }
            resultat.size shouldBe 2
            resultat shouldContainExactlyInAnyOrder listOf(
                rolf.identitetsnummer,
                turid.identitetsnummer
            )
        }

        "Eldste adressebeskyttelse for aktive brukere skal brukes som cache-status" {
            val status = hentAdressebeskyttelseCacheStatus()

            status.eldsteOppdatering shouldBe tidspunkt - Duration.ofHours(12)
            status.manglerFlagg shouldBe false
        }

        "Manglende adressebeskyttelsesflagg for aktiv bruker skal oppdages" {
            transaction {
                BrukerFlaggTable.deleteWhere {
                    (BrukerFlaggTable.navn eq HarBeskyttetAdresseFlaggtype.type) and
                        (BrukerFlaggTable.brukerId eq olaBrukerId)
                }
            }

            hentAdressebeskyttelseCacheStatus().manglerFlagg shouldBe true
        }
    }
})
