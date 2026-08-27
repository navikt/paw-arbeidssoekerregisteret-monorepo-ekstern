package no.naw.paw.minestillinger

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.naw.paw.minestillinger.brukerprofil.SlettGamlePropfileringerUtenProfil
import no.naw.paw.minestillinger.brukerprofil.SlettUbrukteBrukerprofiler
import no.naw.paw.minestillinger.brukerprofil.beskyttetadresse.BeskyttetAddresseDagligOppdatering
import no.naw.paw.minestillinger.brukerprofil.direktemeldte.DirektemeldteStillingerFlaggOppdatering
import no.naw.paw.minestillinger.metrics.AntallBrukereMetrics
import java.time.Duration
import java.util.Collections.synchronizedSet
import java.util.concurrent.Executors

class BakgrunnsprosesserTest : FreeSpec({
    "oppstartsforsinkelse holder liveness grønn og readiness rød" {
        val adresse = mockk<BeskyttetAddresseDagligOppdatering>(relaxed = true)
        val slettBrukere = mockk<SlettUbrukteBrukerprofiler>(relaxed = true)
        val slettProfileringer = mockk<SlettGamlePropfileringerUtenProfil>(relaxed = true)
        val metrics = mockk<AntallBrukereMetrics>(relaxed = true)
        val direktemeldte = mockk<DirektemeldteStillingerFlaggOppdatering>(relaxed = true)
        val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        val bakgrunnsprosesser = Bakgrunnsprosesser(
            adresseBeskyttelseOppdatering = adresse,
            slettUbrukteBrukerprofiler = slettBrukere,
            slettGamlePropfileringerUtenProfil = slettProfileringer,
            antallBrukereMetrics = metrics,
            inkluderDirektemeldteStillingerFlaggOppdatering = direktemeldte,
            startupDelay = { Duration.ofHours(1) },
            dispatcher = dispatcher
        )

        try {
            bakgrunnsprosesser.start()

            bakgrunnsprosesser.hasStarted() shouldBe true
            bakgrunnsprosesser.isAlive() shouldBe true
            bakgrunnsprosesser.isReady() shouldBe false
        } finally {
            bakgrunnsprosesser.close()
        }
    }

    "alle bakgrunnsjobber kjører på samme tråd og stoppes kontrollert" {
        val adresse = mockk<BeskyttetAddresseDagligOppdatering>(relaxed = true)
        val slettBrukere = mockk<SlettUbrukteBrukerprofiler>(relaxed = true)
        val slettProfileringer = mockk<SlettGamlePropfileringerUtenProfil>(relaxed = true)
        val metrics = mockk<AntallBrukereMetrics>(relaxed = true)
        val direktemeldte = mockk<DirektemeldteStillingerFlaggOppdatering>(relaxed = true)
        val tråder = synchronizedSet(mutableSetOf<Long>())

        coEvery { adresse.start() } coAnswers { tråder.add(Thread.currentThread().threadId()); awaitCancellation() }
        coEvery { slettBrukere.start() } coAnswers { tråder.add(Thread.currentThread().threadId()); awaitCancellation() }
        coEvery { slettProfileringer.start() } coAnswers { tråder.add(Thread.currentThread().threadId()); awaitCancellation() }
        coEvery { metrics.startPeriodiskOppdateringAvMetrics() } coAnswers {
            tråder.add(Thread.currentThread().threadId())
            awaitCancellation()
        }
        coEvery { direktemeldte.start() } coAnswers {
            tråder.add(Thread.currentThread().threadId())
            awaitCancellation()
        }
        every { adresse.isAlive() } returns true
        every { adresse.isReady() } returns true

        val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        val bakgrunnsprosesser = Bakgrunnsprosesser(
            adresseBeskyttelseOppdatering = adresse,
            slettUbrukteBrukerprofiler = slettBrukere,
            slettGamlePropfileringerUtenProfil = slettProfileringer,
            antallBrukereMetrics = metrics,
            inkluderDirektemeldteStillingerFlaggOppdatering = direktemeldte,
            startupDelay = { Duration.ZERO },
            dispatcher = dispatcher
        )

        try {
            bakgrunnsprosesser.hasStarted() shouldBe false
            bakgrunnsprosesser.start()
            runBlocking {
                repeat(100) {
                    if (tråder.isNotEmpty() && bakgrunnsprosesser.isAlive()) return@runBlocking
                    delay(10)
                }
            }

            bakgrunnsprosesser.hasStarted() shouldBe true
            bakgrunnsprosesser.isAlive() shouldBe true
            bakgrunnsprosesser.isReady() shouldBe true
            tråder shouldHaveSize 1
        } finally {
            bakgrunnsprosesser.close()
        }

        verify(exactly = 1) { adresse.close() }
        verify(exactly = 1) { slettBrukere.close() }
        verify(exactly = 1) { slettProfileringer.close() }
        verify(exactly = 1) { direktemeldte.close() }
    }
})
