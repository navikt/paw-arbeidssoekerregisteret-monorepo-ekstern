package no.nav.paw.arbeidssoekerregisteret.kontrakt

import io.kotest.assertions.json.ArrayOrder
import io.kotest.assertions.json.FieldComparison
import io.kotest.assertions.json.NumberFormat
import io.kotest.assertions.json.PropertyOrder
import io.kotest.assertions.json.TypeCoercion
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.style.FreeSpec
import io.kotest.core.spec.style.scopes.FreeSpecContainerScope
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssoekerregisteret.model.MicroFrontend
import no.nav.paw.arbeidssoekerregisteret.model.PeriodeInfo
import no.nav.paw.arbeidssoekerregisteret.model.Sensitivitet
import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import no.nav.paw.arbeidssoekerregisteret.model.ToggleAction
import no.nav.paw.arbeidssoekerregisteret.utils.buildPeriodeInfoSerde
import no.nav.paw.arbeidssoekerregisteret.utils.buildToggleSerde
import org.apache.kafka.common.serialization.Serde
import java.time.Instant
import java.util.UUID

/**
 * JSON-kontrakttester (fasitfiler) for data som serialiseres med produksjonens serdes.
 *
 * - [PeriodeInfo] lagres i Kafka Streams state store (`PeriodeStateStore`) via `buildPeriodeInfoSerde()`.
 * - [Toggle] produseres til microfrontend-topic via `buildToggleSerde()`.
 *
 * Fasitfilene under `src/test/resources/json-kontrakt/` må aldri endres. Feiler en test her, er formatet
 * endret. For lagret data (state store) legges nytt format i en ny fil, og de gamle filene beholdes
 * slik at vi fortsatt beviser at gammel data kan leses.
 */
class JsonKontraktTest : FreeSpec({
    val periodeInfoSerde = buildPeriodeInfoSerde()
    val toggleSerde = buildToggleSerde()

    "PeriodeInfo i state store" - {
        val id = UUID.fromString("5a0e1d2c-3b4f-4a6e-8c9d-0e1f2a3b4c5d")
        "aktiv periode (avsluttet = null)" - {
            sjekkKontrakt(
                serde = periodeInfoSerde,
                fil = "json-kontrakt/periode-state-store/periode-info-aktiv.json",
                eksempel = PeriodeInfo(
                    id = id,
                    identitetsnummer = "01017012345",
                    arbeidssoekerId = 1234567L,
                    startet = Instant.parse("2026-01-15T10:15:30.123456Z"),
                    avsluttet = null
                )
            )
        }
        "avsluttet periode" - {
            sjekkKontrakt(
                serde = periodeInfoSerde,
                fil = "json-kontrakt/periode-state-store/periode-info-avsluttet.json",
                eksempel = PeriodeInfo(
                    id = id,
                    identitetsnummer = "02017012345",
                    arbeidssoekerId = 7654321L,
                    startet = Instant.parse("2026-01-15T10:15:30.123456Z"),
                    avsluttet = Instant.parse("2026-02-20T08:45:10.987654321Z")
                )
            )
        }
    }

    "Toggle til microfrontend-topic" - {
        "enable med sensitivitet high" - {
            sjekkKontrakt(
                serde = toggleSerde,
                fil = "json-kontrakt/microfrontend-toggle/toggle-enable-high.json",
                eksempel = Toggle(
                    action = ToggleAction.ENABLE,
                    ident = "01017012345",
                    microfrontendId = MicroFrontend.AIA_MIN_SIDE,
                    sensitivitet = Sensitivitet.HIGH,
                    initiatedBy = "paw"
                )
            )
        }
        "enable med sensitivitet substantial" - {
            sjekkKontrakt(
                serde = toggleSerde,
                fil = "json-kontrakt/microfrontend-toggle/toggle-enable-substantial.json",
                eksempel = Toggle(
                    action = ToggleAction.ENABLE,
                    ident = "01017012345",
                    microfrontendId = MicroFrontend.AIA_MIN_SIDE,
                    sensitivitet = Sensitivitet.SUBSTANTIAL,
                    initiatedBy = "paw"
                )
            )
        }
        "disable uten sensitivitet" - {
            sjekkKontrakt(
                serde = toggleSerde,
                fil = "json-kontrakt/microfrontend-toggle/toggle-disable.json",
                eksempel = Toggle(
                    action = ToggleAction.DISABLE,
                    ident = "01017012345",
                    microfrontendId = MicroFrontend.AIA_MIN_SIDE,
                    sensitivitet = null,
                    initiatedBy = "paw"
                )
            )
        }
    }
})

private suspend fun <T> FreeSpecContainerScope.sjekkKontrakt(serde: Serde<T>, fil: String, eksempel: T) {
    val fasit = lesRessurs(fil)
    "serialisering gir JSON lik fasitfil" {
        val produsert = serde.serializer().serialize("topic", eksempel).toString(Charsets.UTF_8)
        produsert shouldEqualJson {
            propertyOrder = PropertyOrder.Strict
            fieldComparison = FieldComparison.Strict
            numberFormat = NumberFormat.Strict
            arrayOrder = ArrayOrder.Strict
            typeCoercion = TypeCoercion.Disabled
            fasit
        }
    }
    "deserialisering av fasitfil gir eksempelobjektet" {
        serde.deserializer().deserialize("topic", fasit.toByteArray(Charsets.UTF_8)) shouldBe eksempel
    }
}

private fun lesRessurs(fil: String): String =
    requireNotNull(JsonKontraktTest::class.java.classLoader.getResource(fil)) { "Fant ikke fasitfil $fil" }
        .readText(Charsets.UTF_8)
