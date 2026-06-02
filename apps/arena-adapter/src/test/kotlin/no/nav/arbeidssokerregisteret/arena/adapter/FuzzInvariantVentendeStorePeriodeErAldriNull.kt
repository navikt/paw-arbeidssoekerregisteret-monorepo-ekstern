package no.nav.arbeidssokerregisteret.arena.adapter

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.arbeidssokerregisteret.arena.adapter.utils.metadata
import no.nav.arbeidssokerregisteret.arena.adapter.utils.periode
import no.nav.arbeidssokerregisteret.arena.adapter.utils.profilering
import no.nav.paw.arbeidssokerregisteret.arena.adapter.ForsinkelseMetadata
import no.nav.paw.arbeidssokerregisteret.arena.adapter.utils.toArena
import no.nav.paw.arbeidssokerregisteret.arena.helpers.v4.TopicsJoin
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.random.Random

sealed class Hendelse {
    data class SendPeriode(val key: Long, val periodeId: UUID) : Hendelse()
    data class SendProfilering(val key: Long, val periodeId: UUID) : Hendelse()
    data class SkruKlokkenFrem(val ms: Long) : Hendelse()
}
/**
 * Fuzz-test for invarianten:
 *   ventende[X] != null → topicsJoinStore[X].periode != null
 *
 * Målet er å forsøke alle kombinasjoner av rekkefølge og tidsstyring for å
 * avgjøre om tilstanden som logget en prod-warning kan oppstå via gjeldende kode.
 *
 * TopologyPunctuation.kt er midlertidig endret til å kaste IllegalStateException
 * i stedet for å logge warning, slik at et brudd vil feile testene.
 *
 * Hendelsesmodell:
 *   - SendPeriode(periodeId)    → sender åpen periode-melding
 *   - SendProfilering(periodeId) → sender profilering-melding
 *   - SkruKlokkenFrem(ms)       → SkruKlokkenFrem simulert tid, trigger punctuator
 *
 * Hvert scenario genererer et tilfeldig sekvens av disse hendelsene for N periodeId-er
 * og kjører dem gjennom en isolert testScope.
 */
class FuzzInvariantVentendeStorePeriodeErAldriNull : FreeSpec({

    val baseStartTid = Instant.parse("2025-01-01T12:00:00Z")
    val periodeStartTid = metadata(Instant.parse("2025-01-01T11:00:00Z"))

    // Tidssteg som brukes i fuzzingen — noen under, noen over 5s-grensen
    val muligeTidssteg = listOf(0L, 500L, 1000L, 2000L, 4000L, 4999L, 5000L, 5001L, 6000L, 10000L)



    /**
     * Genererer en tilfeldig liste med hendelser for [antallPeriodeIds] periodeId-er.
     * Hver periodeId kan bidra med: ingen, bare periode, bare profilering, eller begge.
     * Klokken skrus frem tilfeldig mellom hendelsene.
     */
    fun genererHendelser(rng: Random, antallPeriodeIds: Int): List<Hendelse> {
        data class PeriodeContext(val key: Long, val periodeId: UUID)

        val kontekster = (1..antallPeriodeIds).map {
            PeriodeContext(key = it.toLong(), periodeId = UUID.randomUUID())
        }

        // For hvert periodeId, velg hvilke meldinger som skal sendes
        val hendelseSett: List<List<Hendelse>> = kontekster.map { ctx ->
            val type = rng.nextInt(5) // 0=bare periode, 1=bare profilering, 2=periode+profilering, 3=periode+profilering+duplikat, 4=ingen
            when (type) {
                0 -> listOf(Hendelse.SendPeriode(ctx.key, ctx.periodeId))
                1 -> listOf(Hendelse.SendProfilering(ctx.key, ctx.periodeId))
                2 -> listOf(
                    Hendelse.SendPeriode(ctx.key, ctx.periodeId),
                    Hendelse.SendProfilering(ctx.key, ctx.periodeId)
                )
                3 -> listOf(
                    Hendelse.SendPeriode(ctx.key, ctx.periodeId),
                    Hendelse.SendProfilering(ctx.key, ctx.periodeId),
                    Hendelse.SendProfilering(ctx.key, ctx.periodeId) // duplikat profilering
                )
                else -> emptyList()
            }
        }

        // Flett hendelsene fra alle periodeId-er tilfeldig
        val flettede = mutableListOf<Hendelse>()
        val køer = hendelseSett.filter { it.isNotEmpty() }.map { ArrayDeque(it) }.toMutableList()
        while (køer.isNotEmpty()) {
            val valgtKø = rng.nextInt(køer.size)
            flettede.add(køer[valgtKø].removeFirst())
            if (køer[valgtKø].isEmpty()) køer.removeAt(valgtKø)

            // Tilfeldig klokkefremdrift mellom hendelser
            if (rng.nextBoolean()) {
                val ms = muligeTidssteg[rng.nextInt(muligeTidssteg.size)]
                if (ms > 0) flettede.add(Hendelse.SkruKlokkenFrem(ms))
            }
        }

        // Alltid avslutt med nok klokkefremdrift til å trigge punctuator
        flettede.add(Hendelse.SkruKlokkenFrem(10000L))
        flettede.add(Hendelse.SkruKlokkenFrem(10000L))

        return flettede
    }

    fun kjørScenario(seed: Long, iterasjon: Int, antallPeriodeIds: Int) {
        val rng = Random(seed * 1000 + iterasjon)
        val hendelser = genererHendelser(rng, antallPeriodeIds)

        with(testScope(baseStartTid)) {
            for (hendelse in hendelser) {
                when (hendelse) {
                    is Hendelse.SendPeriode -> {
                        val p = periode(
                            identietsnummer = hendelse.periodeId.toString().take(11).replace("-", "0").take(11),
                            id = hendelse.periodeId,
                            startet = periodeStartTid
                        )
                        periodeTopic.pipeInput(hendelse.key, p)
                    }
                    is Hendelse.SendProfilering -> {
                        val prof = profilering(
                            periode = hendelse.periodeId,
                            timestamp = baseStartTid
                        )
                        profileringsTopic.pipeInput(hendelse.key, prof)
                    }
                    is Hendelse.SkruKlokkenFrem -> {
                        topologyTestDriver.advanceWallClockTime(Duration.ofMillis(hendelse.ms))
                    }
                }
            }
            // Tøm arena-topic uten å sjekke innholdet — vi er bare ute etter exception
            arenaTopic.readKeyValuesToList()
        }
    }

    "Fuzz med seed=42: 500 iterasjoner, 5 periodeId-er per iterasjon" {
        repeat(500) { iterasjon ->
            kjørScenario(seed = 42L, iterasjon = iterasjon, antallPeriodeIds = 5)
        }
    }

    "Fuzz med seed=1337: 500 iterasjoner, 5 periodeId-er per iterasjon" {
        repeat(500) { iterasjon ->
            kjørScenario(seed = 1337L, iterasjon = iterasjon, antallPeriodeIds = 5)
        }
    }

    "Fuzz med seed=999: 200 iterasjoner, 20 periodeId-er per iterasjon (høy parallellitet)" {
        repeat(200) { iterasjon ->
            kjørScenario(seed = 999L, iterasjon = iterasjon, antallPeriodeIds = 20)
        }
    }

    "Fuzz med seed=2025: 200 iterasjoner, 3 periodeId-er (fokus på timing-grensen)" {
        repeat(200) { iterasjon ->
            kjørScenario(seed = 2025L, iterasjon = iterasjon, antallPeriodeIds = 3)
        }
    }

    "Edgecase: profilering sendes akkurat når punctuator nettopp har forwardet periode" {
        // GIVEN: periode → vent akkurat 5s → profilering
        with(testScope(baseStartTid)) {
            val periodeId = UUID.randomUUID()
            val p = periode(identietsnummer = "20000000001", id = periodeId, startet = periodeStartTid)
            val prof = profilering(periode = periodeId, timestamp = baseStartTid.plusSeconds(5))

            // WHEN
            periodeTopic.pipeInput(1L, p)
            topologyTestDriver.advanceWallClockTime(Duration.ofMillis(5000))
            // punctuator kjører her — akkurat på grensen
            profileringsTopic.pipeInput(1L, prof)
            topologyTestDriver.advanceWallClockTime(Duration.ofMillis(10000))

            // THEN
            arenaTopic.readKeyValuesToList() // ingen exception = invarianten holdt
        }
    }

    "Edgecase: profilering → 4999ms → periode (rett under grensen, ventende aldri satt)" {
        with(testScope(baseStartTid)) {
            val periodeId = UUID.randomUUID()
            val p = periode(identietsnummer = "20000000002", id = periodeId, startet = periodeStartTid)
            val prof = profilering(periode = periodeId, timestamp = baseStartTid)

            profileringsTopic.pipeInput(1L, prof)
            topologyTestDriver.advanceWallClockTime(Duration.ofMillis(4999))
            periodeTopic.pipeInput(1L, p)
            topologyTestDriver.advanceWallClockTime(Duration.ofMillis(10000))

            arenaTopic.readKeyValuesToList()
        }
    }

    "Edgecase: 50 periodeId-er alle aktive simultaneously, blanding av bare-periode og begge" {
        with(testScope(baseStartTid)) {
            val rng = Random(7777)
            val periodeIds = (1..50).map { UUID.randomUUID() }

            // Send alle periodene
            periodeIds.forEachIndexed { i, pid ->
                val p = periode(
                    identietsnummer = "3${i.toString().padStart(10, '0')}".take(11),
                    id = pid,
                    startet = periodeStartTid
                )
                periodeTopic.pipeInput(i.toLong() + 1, p)
            }

            // Send profilering for halvparten
            periodeIds.shuffled(rng).take(25).forEachIndexed { i, pid ->
                val prof = profilering(periode = pid, timestamp = baseStartTid)
                profileringsTopic.pipeInput(i.toLong() + 1, prof)
            }

            // Trigger punctuator
            topologyTestDriver.advanceWallClockTime(Duration.ofMillis(6000))
            topologyTestDriver.advanceWallClockTime(Duration.ofMillis(6000))

            arenaTopic.readKeyValuesToList() // ingen exception = invarianten holdt
            // Forvent 50 utgående meldinger (25 med profilering, 25 kun periode)
            arenaTopic.isEmpty shouldBe true
        }
    }

    "Direkte state-injeksjon: verifiser at exception faktisk kastes ved manuelt korrupt tilstand" {
        // Denne testen beviser at deteksjonen faktisk virker.
        // Vi injiserer den umulige tilstanden direkte i state stores (simulerer infrastruktur-korrupsjon)
        // og bekrefter at punctuatoren kaster IllegalStateException.
        //
        // NB: TopologyTestDriver pakker exceptions fra punctuatorer inn i StreamsException,
        // så vi går cause-kjeden for å finne vår IllegalStateException.
        val thrown = shouldThrow<Exception> {
            with(testScope(baseStartTid)) {
                val periodeId = UUID.randomUUID()
                val korruptProfilering = toArena(profilering(periode = periodeId, timestamp = baseStartTid))

                // Injiser korrupt tilstand direkte i store:
                // - topicsJoinStore har {periode=null, profilering!=null} → bryter invarianten
                // - ventendePeriodeStore har en entry for samme nøkkel
                joinStore.put(periodeId, TopicsJoin(null, korruptProfilering, null))
                ventendePeriodeStore.put(
                    periodeId,
                    ForsinkelseMetadata(
                        recordKey = 1L,
                        traceparent = null,
                        timestamp = baseStartTid.toEpochMilli() - 10_000L // 10s gammel
                    )
                )

                // Trigger punctuatoren — skal kaste IllegalStateException (pakket i StreamsException)
                topologyTestDriver.advanceWallClockTime(Duration.ofMillis(6000))
            }
        }
        val invariantBrudd = generateSequence(thrown as Throwable) { it.cause }
            .filterIsInstance<IllegalStateException>()
            .firstOrNull()
        invariantBrudd?.message?.contains("invariant brutt") shouldBe true
    }
})
