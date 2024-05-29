package no.nav.paw.arbeidssoekerregisteret.model

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssoekerregisteret.config.buildObjectMapper

class ToggleTest : FreeSpec({
    with(ToggleTestContext()) {
        "Test suite for serialisering og deserialisering av Toggle" - {
            "Skal serialisere enable Toggle korrekt" {
                val toggle = buildEnableToggle("12345678901", "aia-min-side")
                val jsonToggle = objectMapper.writeValueAsString(toggle)
                jsonToggle shouldBe enableToggleJsonString
            }
            "Skal serialisere disable Toggle korrekt" {
                val toggle = buildDisableToggle("12345678901", "aia-min-side")
                val jsonToggle = objectMapper.writeValueAsString(toggle)
                jsonToggle shouldBe disableToggleJsonString
            }
        }
    }
})

class ToggleTestContext {
    val objectMapper = buildObjectMapper
    val enableToggleJsonString =
        """
           {"@action":"enable","ident":"12345678901","microfrontend_id":"aia-min-side","sensitivitet":"high","@initiated_by":"paw"}
        """.trimIndent()
    val disableToggleJsonString =
        """
           {"@action":"disable","ident":"12345678901","microfrontend_id":"aia-min-side","@initiated_by":"paw"}
        """.trimIndent()
}