package no.naw.paw.minestillinger.brukerprofil.beskyttetadresse

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.naw.paw.minestillinger.db.ops.AdressebeskyttelseCacheStatus
import java.time.Duration
import java.time.Instant

class AdressebeskyttelseCacheHelseTest : FreeSpec({
    val nå = Instant.parse("2026-08-27T07:00:00Z")

    "cache uten aktive brukere er fersk" {
        erAdressebeskyttelseCacheFersk(
            AdressebeskyttelseCacheStatus(eldsteOppdatering = null, manglerFlagg = false),
            nå
        ) shouldBe true
    }

    "cache med manglende flagg er ikke fersk" {
        erAdressebeskyttelseCacheFersk(
            AdressebeskyttelseCacheStatus(eldsteOppdatering = nå, manglerFlagg = true),
            nå
        ) shouldBe false
    }

    "cache på 36 timer er fersk" {
        erAdressebeskyttelseCacheFersk(
            AdressebeskyttelseCacheStatus(
                eldsteOppdatering = nå - Duration.ofHours(36),
                manglerFlagg = false
            ),
            nå
        ) shouldBe true
    }

    "cache eldre enn 36 timer er ikke fersk" {
        erAdressebeskyttelseCacheFersk(
            AdressebeskyttelseCacheStatus(
                eldsteOppdatering = nå - Duration.ofHours(36) - Duration.ofMillis(1),
                manglerFlagg = false
            ),
            nå
        ) shouldBe false
    }

    "fremgang eldre enn 12 timer er ikke nylig" {
        erFremgangNylig(nå - Duration.ofHours(12) - Duration.ofMillis(1), nå) shouldBe false
    }

    "fremgang på 12 timer er nylig" {
        erFremgangNylig(nå - Duration.ofHours(12), nå) shouldBe true
    }

    "cache-status eldre enn 30 minutter er ikke nylig" {
        erCacheStatusNylig(nå - Duration.ofMinutes(30) - Duration.ofMillis(1), nå) shouldBe false
    }

    "cache-status på 30 minutter er nylig" {
        erCacheStatusNylig(nå - Duration.ofMinutes(30), nå) shouldBe true
    }
})
