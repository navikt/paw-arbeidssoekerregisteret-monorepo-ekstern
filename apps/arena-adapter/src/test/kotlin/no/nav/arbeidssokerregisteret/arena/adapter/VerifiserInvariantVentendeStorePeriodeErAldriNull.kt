package no.nav.arbeidssokerregisteret.arena.adapter

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.arbeidssokerregisteret.arena.adapter.utils.metadata
import no.nav.arbeidssokerregisteret.arena.adapter.utils.periode
import no.nav.arbeidssokerregisteret.arena.adapter.utils.profilering
import java.time.Duration
import java.time.Instant
import java.util.*

/**
 * Forsøker å trigge invariant-bruddet der punctuatoren finner en nøkkel i ventendePeriodeStore
 * men topicsJoinStore har periode == null for samme nøkkel.
 *
 * Invarianten som skal holde: ventende[X] != null → store[X].periode != null
 *
 * TopologyPunctuation er midlertidig endret til å kaste IllegalStateException i stedet for
 * å logge warning, slik at testen kan detektere et eventuelt brudd.
 *
 * Hver test bruker sin egen testScope for full isolasjon.
 */
class VerifiserInvariantVentendeStorePeriodeErAldriNull : FreeSpec({

    val startTid = Instant.parse("2025-01-01T12:00:00Z")
    val periodeStartet = metadata(Instant.parse("2025-01-01T11:00:00Z"))
    val KEY = 1L

    fun nyttPeriodeId() = UUID.randomUUID()

    "Kun periode — punctuator kjører etter 5s" {
        // GIVEN
        with(testScope(startTid)) {
            val periodeId = nyttPeriodeId()
            val p = periode(identietsnummer = "10000000001", id = periodeId, startet = periodeStartet)

            // WHEN
            periodeTopic.pipeInput(KEY, p)
            // ventende[periodeId] != null, store[periodeId].periode != null → invariant OK

            topologyTestDriver.advanceWallClockTime(Duration.ofSeconds(6))
            // punctuator kjører: store[periodeId].periode != null → forward, ingen exception

            // THEN
            arenaTopic.isEmpty shouldBe false
            arenaTopic.readKeyValue().value.periode.id shouldBe periodeId
            arenaTopic.isEmpty shouldBe true
        }
    }

    "Periode → profilering innen 5s → punctuator etter 5s (ventende ryddet av profilering)" {
        // GIVEN
        with(testScope(startTid)) {
            val periodeId = nyttPeriodeId()
            val p = periode(identietsnummer = "10000000002", id = periodeId, startet = periodeStartet)
            val prof = profilering(periode = periodeId, timestamp = startTid.plusSeconds(3))

            // WHEN
            periodeTopic.pipeInput(KEY, p)
            topologyTestDriver.advanceWallClockTime(Duration.ofSeconds(3))
            profileringsTopic.pipeInput(KEY, prof)
            // profilering slettet ventende[periodeId], forwardet med begge deler

            topologyTestDriver.advanceWallClockTime(Duration.ofSeconds(6))
            // punctuator kjører: ventende er tom → ingen iterasjon → ingen exception

            // THEN
            arenaTopic.isEmpty shouldBe false
            arenaTopic.readKeyValue().value.let { melding ->
                melding.periode.id shouldBe periodeId
                melding.profilering.periodeId shouldBe periodeId
            }
            arenaTopic.isEmpty shouldBe true
        }
    }

    "Periode → punctuator etter 5s → profilering ankommer (to meldinger til arena)" {
        // GIVEN
        with(testScope(startTid)) {
            val periodeId = nyttPeriodeId()
            val p = periode(identietsnummer = "10000000003", id = periodeId, startet = periodeStartet)
            val prof = profilering(periode = periodeId, timestamp = startTid.plusSeconds(10))

            // WHEN
            periodeTopic.pipeInput(KEY, p)
            topologyTestDriver.advanceWallClockTime(Duration.ofSeconds(6))
            // punctuator: store.periode != null → forward, ventende.delete → ingen exception

            arenaTopic.isEmpty shouldBe false
            arenaTopic.readKeyValuesToList() // tøm output

            profileringsTopic.pipeInput(KEY, prof)
            // profilering: existingValue.profilering==null, periode!=null → ventende.delete (no-op), forward

            topologyTestDriver.advanceWallClockTime(Duration.ofSeconds(6))
            // punctuator: ventende er tom → ingen iterasjon → ingen exception

            // THEN
            arenaTopic.isEmpty shouldBe false
            arenaTopic.readKeyValue().value.let { melding ->
                melding.periode.id shouldBe periodeId
                melding.profilering.periodeId shouldBe periodeId
            }
            arenaTopic.isEmpty shouldBe true
        }
    }

    "Kun profilering — ingen periode — punctuator etter 5s (aldri noen ventende-entry)" {
        // GIVEN
        with(testScope(startTid)) {
            val periodeId = nyttPeriodeId()
            val prof = profilering(periode = periodeId, timestamp = startTid)

            // WHEN
            profileringsTopic.pipeInput(KEY, prof)
            // store[periodeId] = {null, profilering}, ingen ventende-entry

            topologyTestDriver.advanceWallClockTime(Duration.ofSeconds(6))
            // punctuator: ventende er tom → ingen iterasjon → ingen exception

            // THEN: ingenting sendt til arena (periode mangler)
            arenaTopic.isEmpty shouldBe true
        }
    }

    "Profilering → periode innen 5s → punctuator etter 5s (ingen ventende-entry)" {
        // GIVEN
        with(testScope(startTid)) {
            val periodeId = nyttPeriodeId()
            val p = periode(identietsnummer = "10000000005", id = periodeId, startet = periodeStartet)
            val prof = profilering(periode = periodeId, timestamp = startTid)

            // WHEN
            profileringsTopic.pipeInput(KEY, prof)
            // store = {null, profilering}, ingen ventende
            topologyTestDriver.advanceWallClockTime(Duration.ofSeconds(3))
            periodeTopic.pipeInput(KEY, p)
            // periode: store[X].periode==null → store.put({p,prof}), profilering!=null → forward, ingen putIfAbsent

            topologyTestDriver.advanceWallClockTime(Duration.ofSeconds(6))
            // punctuator: ventende er tom → ingen iterasjon → ingen exception

            // THEN
            arenaTopic.isEmpty shouldBe false
            arenaTopic.readKeyValue().value.let { melding ->
                melding.periode.id shouldBe periodeId
                melding.profilering.periodeId shouldBe periodeId
            }
            arenaTopic.isEmpty shouldBe true
        }
    }

    "Profilering → punctuator etter 5s → periode (punctuator berøres ikke, periode forwardes direkte)" {
        // GIVEN
        with(testScope(startTid)) {
            val periodeId = nyttPeriodeId()
            val p = periode(identietsnummer = "10000000006", id = periodeId, startet = periodeStartet)
            val prof = profilering(periode = periodeId, timestamp = startTid)

            // WHEN
            profileringsTopic.pipeInput(KEY, prof)
            // store = {null, profilering}, ingen ventende

            topologyTestDriver.advanceWallClockTime(Duration.ofSeconds(6))
            // punctuator: ventende er tom → ingen iterasjon → ingen exception

            periodeTopic.pipeInput(KEY, p)
            // periode: store[X].periode==null → store.put({p,prof}), profilering!=null → forward, ingen putIfAbsent

            topologyTestDriver.advanceWallClockTime(Duration.ofSeconds(6))
            // punctuator: ventende er tom → ingen iterasjon → ingen exception

            // THEN
            arenaTopic.isEmpty shouldBe false
            arenaTopic.readKeyValue().value.let { melding ->
                melding.periode.id shouldBe periodeId
                melding.profilering.periodeId shouldBe periodeId
            }
            arenaTopic.isEmpty shouldBe true
        }
    }

    "Duplikat periode — andre periode-melding er idempotent — punctuator etter 5s" {
        // GIVEN
        with(testScope(startTid)) {
            val periodeId = nyttPeriodeId()
            val p = periode(identietsnummer = "10000000007", id = periodeId, startet = periodeStartet)

            // WHEN
            periodeTopic.pipeInput(KEY, p)
            periodeTopic.pipeInput(KEY, p) // andre melding: store[X].periode!=null → ingenting (idempotent)

            topologyTestDriver.advanceWallClockTime(Duration.ofSeconds(6))
            // punctuator: store.periode != null → forward, ingen exception

            // THEN
            arenaTopic.isEmpty shouldBe false
            arenaTopic.readKeyValue().value.periode.id shouldBe periodeId
            arenaTopic.isEmpty shouldBe true
        }
    }

    "Duplikat profilering — andre profilering-melding er idempotent — punctuator etter 5s" {
        // GIVEN
        with(testScope(startTid)) {
            val periodeId = nyttPeriodeId()
            val p = periode(identietsnummer = "10000000008", id = periodeId, startet = periodeStartet)
            val prof = profilering(periode = periodeId, timestamp = startTid.plusSeconds(1))

            // WHEN
            periodeTopic.pipeInput(KEY, p)
            topologyTestDriver.advanceWallClockTime(Duration.ofSeconds(1))
            profileringsTopic.pipeInput(KEY, prof)
            // profilering: ventende.delete, forward

            profileringsTopic.pipeInput(KEY, prof) // andre melding: existingValue.profilering!=null → ingenting
            arenaTopic.readKeyValuesToList() // tøm

            topologyTestDriver.advanceWallClockTime(Duration.ofSeconds(6))
            // punctuator: ventende er tom → ingen iterasjon → ingen exception

            // THEN
            arenaTopic.isEmpty shouldBe true
        }
    }

    "Periode → profilering innen 5s → ny punctuator-runde langt etter (ventende forblir tom)" {
        // GIVEN
        with(testScope(startTid)) {
            val periodeId = nyttPeriodeId()
            val p = periode(identietsnummer = "10000000009", id = periodeId, startet = periodeStartet)
            val prof = profilering(periode = periodeId, timestamp = startTid.plusSeconds(2))

            // WHEN
            periodeTopic.pipeInput(KEY, p)
            topologyTestDriver.advanceWallClockTime(Duration.ofSeconds(2))
            profileringsTopic.pipeInput(KEY, prof)
            arenaTopic.readKeyValuesToList() // tøm

            // Kjør punctuator mange ganger langt frem i tid
            repeat(10) {
                topologyTestDriver.advanceWallClockTime(Duration.ofSeconds(10))
            }
            // Ingen exception, ingenting i ventende

            // THEN
            arenaTopic.isEmpty shouldBe true
        }
    }
})
