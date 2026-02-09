package no.nav.paw.arbeidssoekerregisteret.model

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssoekerregisteret.utils.buildObjectMapper
import no.nav.paw.felles.model.Identitetsnummer

class ToggleTest : FreeSpec({
    with(ToggleTestContext()) {
        "Test suite for serialisering og deserialisering av Toggle" - {
            "Skal serialisere enable Toggle korrekt" {
                val toggle = Toggle(
                    action = ToggleAction.ENABLE,
                    ident = "01017012345",
                    microfrontendId = MicroFrontend.AIA_MIN_SIDE,
                    sensitivitet = Sensitivitet.HIGH,
                    initiatedBy = "paw"
                )
                val jsonToggle = objectMapper.writeValueAsString(toggle)
                jsonToggle shouldBe enableToggleJsonString
            }
            "Skal serialisere disable Toggle korrekt" {
                val toggle = Toggle(
                    action = ToggleAction.DISABLE,
                    ident = "01017012345",
                    microfrontendId = MicroFrontend.AIA_MIN_SIDE,
                    initiatedBy = "paw"
                )
                val jsonToggle = objectMapper.writeValueAsString(toggle)
                jsonToggle shouldBe disableToggleJsonString
            }
            "Skal serialisere enable ToggleRequest korrekt" {
                val toggle = ToggleRequest(
                    action = ToggleAction.ENABLE,
                    microfrontendId = MicroFrontend.AIA_MIN_SIDE
                ).asToggle(Identitetsnummer("01017012345"), Sensitivitet.HIGH)
                val jsonToggle = objectMapper.writeValueAsString(toggle)
                jsonToggle shouldBe enableToggleJsonString
            }
            "Skal serialisere disable ToggleRequest korrekt" {
                val toggle = ToggleRequest(
                    action = ToggleAction.DISABLE,
                    microfrontendId = MicroFrontend.AIA_MIN_SIDE
                ).asToggle(Identitetsnummer("01017012345"), Sensitivitet.HIGH)
                val jsonToggle = objectMapper.writeValueAsString(toggle)
                jsonToggle shouldBe disableToggleJsonString
            }
        }
    }
})

private class ToggleTestContext {
    val objectMapper = buildObjectMapper
    val enableToggleJsonString =
        """
           {"@action":"enable","ident":"01017012345","microfrontend_id":"aia-min-side","sensitivitet":"high","@initiated_by":"paw"}
        """.trimIndent()
    val disableToggleJsonString =
        """
           {"@action":"disable","ident":"01017012345","microfrontend_id":"aia-min-side","@initiated_by":"paw"}
        """.trimIndent()
}