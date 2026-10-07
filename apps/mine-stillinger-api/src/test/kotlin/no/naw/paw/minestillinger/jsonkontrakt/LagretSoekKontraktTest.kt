package no.naw.paw.minestillinger.jsonkontrakt

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.naw.paw.minestillinger.db.ops.toClass
import no.naw.paw.minestillinger.domain.Stillingssoek
import no.naw.paw.minestillinger.domain.StillingssoekType

/**
 * JSON-kontrakt for lagret `soek` (jsonb-kolonnen i tabellen `soek`).
 *
 * Bruker den ekte, private `soekObjectMapper` i `db/ops/SoekAdmin.kt` via refleksjon,
 * slik at testen kjører uten database. Synligheten i produksjonskoden er ikke endret.
 * Klassen velges fra `type`-kolonnen med produksjonsfunksjonen [toClass], slik `hentSoek` gjør.
 * Hele databaseveien (lagreSoek/hentSoek) dekkes av [LagretSoekDbKontraktTest].
 *
 * Fasitfilene må aldri endres. Lagrede data finnes i produksjon: et nytt format gir en ny fil,
 * og gamle filer skal bli liggende og fortsatt kunne leses.
 */
class LagretSoekKontraktTest : FreeSpec({
    val soekObjectMapper: ObjectMapper = Class.forName("no.naw.paw.minestillinger.db.ops.SoekAdminKt")
        .getDeclaredField("soekObjectMapper")
        .apply { isAccessible = true }
        .get(null) as ObjectMapper

    fun les(type: StillingssoekType, json: String): Stillingssoek =
        soekObjectMapper.readValue(json, type.toClass().java)

    lagredeSoekFasiter.forEach { (fil, soek) ->
        "Lagret ${soek.soekType} i $fil" - {
            "skrives likt fasitfilen" {
                soekObjectMapper.writeValueAsString(soek) skalVaereLikFasit fil
            }
            "fasitfilen leses til samme søk" {
                les(soek.soekType, JsonKontrakt.lesFasit(fil)) shouldBe soek
            }
        }
    }

    "Lagret søk med ukjent felt kan ikke leses (mapperen feiler på ukjente felter)" {
        val medUkjentFelt = JsonKontrakt.lesFasit("lagret/sted-soek-v1-full.json")
            .replaceFirst("{", "{\"ukjentFelt\":1,")
        shouldThrow<UnrecognizedPropertyException> {
            les(StillingssoekType.STED_SOEK_V1, medUkjentFelt)
        }
    }
})

val lagredeSoekFasiter: List<Pair<String, Stillingssoek>> = listOf(
    "lagret/sted-soek-v1-full.json" to Eksempeldata.stedSoekFull,
    "lagret/sted-soek-v1-tomme-lister.json" to Eksempeldata.stedSoekTomt,
    "lagret/reisevei-soek-v1-full.json" to Eksempeldata.reiseveiSoekFull,
    "lagret/reisevei-soek-v1-tomme-lister.json" to Eksempeldata.reiseveiSoekTomt,
)
