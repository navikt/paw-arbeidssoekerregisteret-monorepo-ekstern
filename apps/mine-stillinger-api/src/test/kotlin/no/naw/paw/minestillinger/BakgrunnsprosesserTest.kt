package no.naw.paw.minestillinger

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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
        val ledervalg = alltidLeder()
        val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        val bakgrunnsprosesser = Bakgrunnsprosesser(
            adresseBeskyttelseOppdatering = adresse,
            slettUbrukteBrukerprofiler = slettBrukere,
            slettGamlePropfileringerUtenProfil = slettProfileringer,
            antallBrukereMetrics = metrics,
            inkluderDirektemeldteStillingerFlaggOppdatering = direktemeldte,
            vedlikeholdsledervalg = ledervalg,
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
        val ledervalg = alltidLeder()
        val tråder = synchronizedSet(mutableSetOf<Long>())

        coEvery { adresse.start() } coAnswers { tråder.add(Thread.currentThread().threadId()); awaitCancellation() }
        coEvery { adresse.overvåkCacheStatus() } coAnswers { awaitCancellation() }
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
            vedlikeholdsledervalg = ledervalg,
            startupDelay = { Duration.ZERO },
            dispatcher = dispatcher
        )

        try {
            bakgrunnsprosesser.hasStarted() shouldBe false
            bakgrunnsprosesser.start()
            ventTil {
                tråder.isNotEmpty() && bakgrunnsprosesser.isAlive()
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

    "pod som ikke er leder starter ingen vedlikeholdsjobber" {
        val adresse = mockk<BeskyttetAddresseDagligOppdatering>(relaxed = true)
        val slettBrukere = mockk<SlettUbrukteBrukerprofiler>(relaxed = true)
        val slettProfileringer = mockk<SlettGamlePropfileringerUtenProfil>(relaxed = true)
        val metrics = mockk<AntallBrukereMetrics>(relaxed = true)
        val direktemeldte = mockk<DirektemeldteStillingerFlaggOppdatering>(relaxed = true)
        coEvery { adresse.overvåkCacheStatus() } coAnswers { awaitCancellation() }
        every { adresse.isReady() } returns true

        val bakgrunnsprosesser = Bakgrunnsprosesser(
            adresseBeskyttelseOppdatering = adresse,
            slettUbrukteBrukerprofiler = slettBrukere,
            slettGamlePropfileringerUtenProfil = slettProfileringer,
            antallBrukereMetrics = metrics,
            inkluderDirektemeldteStillingerFlaggOppdatering = direktemeldte,
            vedlikeholdsledervalg = aldriLeder(),
            startupDelay = { Duration.ZERO },
            leaderRetryInterval = Duration.ofMillis(10),
            dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        )

        try {
            bakgrunnsprosesser.start()
            ventTil {
                bakgrunnsprosesser.isAlive() && bakgrunnsprosesser.isReady()
            }

            bakgrunnsprosesser.isAlive() shouldBe true
            bakgrunnsprosesser.isReady() shouldBe true
            coVerify(exactly = 0) { adresse.start() }
            coVerify(exactly = 0) { slettBrukere.start() }
            coVerify(exactly = 0) { slettProfileringer.start() }
            coVerify(exactly = 0) { metrics.startPeriodiskOppdateringAvMetrics() }
            coVerify(exactly = 0) { direktemeldte.start() }
        } finally {
            bakgrunnsprosesser.close()
        }
    }
})

private fun alltidLeder(): Vedlikeholdsledervalg =
    object : Vedlikeholdsledervalg {
        override fun prøvÅBliLeder(): Lederlås =
            object : Lederlås {
                override fun erGyldig(): Boolean = true
                override fun close() = Unit
            }

        override fun close() = Unit
    }

private fun aldriLeder(): Vedlikeholdsledervalg =
    object : Vedlikeholdsledervalg {
        override fun prøvÅBliLeder(): Lederlås? = null
        override fun close() = Unit
    }

private fun ventTil(condition: () -> Boolean) {
    runBlocking {
        withTimeout(5_000) {
            while (!condition()) {
                delay(10)
            }
        }
    }
}
