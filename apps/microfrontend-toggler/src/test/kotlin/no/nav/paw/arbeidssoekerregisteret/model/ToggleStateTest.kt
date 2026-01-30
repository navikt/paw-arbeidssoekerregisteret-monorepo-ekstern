package no.nav.paw.arbeidssoekerregisteret.model

import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssoekerregisteret.utils.buildObjectMapper
import java.time.Instant

class ToggleStateTest : FreeSpec({
    with(ToggleStateTestTestContext()) {
        "Skal serialisere tom ToggleState" {
            val correctToggleState1 = ToggleState(
                identitetsnummer = "01017012345",
                arbeidssoekerId = 10001,
                microfrontends = mapOf()
            )
            val correctToggleState2 = ToggleState(
                identitetsnummer = "02017012345",
                arbeidssoekerId = 10002,
                microfrontends = mapOf(
                    MicroFrontendId.AIA_MIN_SIDE to MicroFrontendState(
                        status = ToggleStatus.UKJENT_VERDI,
                        updated = Instant.parse("2025-01-01T13:37:00.000Z")
                    )
                )
            )
            val correctToggleState3 = ToggleState(
                identitetsnummer = "03017012345",
                arbeidssoekerId = 10003,
                microfrontends = mapOf(
                    MicroFrontendId.AIA_MIN_SIDE to MicroFrontendState(
                        status = ToggleStatus.ENABLED,
                        updated = Instant.parse("2025-01-01T13:37:00.000Z")
                    ),
                    MicroFrontendId.AIA_BEHOVSVURDERING to MicroFrontendState(
                        status = ToggleStatus.DISABLED,
                        updated = Instant.parse("2025-01-30T13:37:00.000Z")
                    )
                )
            )
            val jsonToggleState1 = objectMapper.writeValueAsString(correctToggleState1)
            val jsonToggleState2 = objectMapper.writeValueAsString(correctToggleState2)
            val jsonToggleState3 = objectMapper.writeValueAsString(correctToggleState3)
            val toggleState1 = objectMapper.readValue<ToggleState>(correctJsonToggleState1)
            val toggleState2 = objectMapper.readValue<ToggleState>(correctJsonToggleState2)
            val toggleState3 = objectMapper.readValue<ToggleState>(correctJsonToggleState3)
            jsonToggleState1 shouldBe correctJsonToggleState1
            jsonToggleState2 shouldBe correctJsonToggleState2
            jsonToggleState3 shouldBe correctJsonToggleState3
            toggleState1 shouldBe correctToggleState1
            toggleState2 shouldBe correctToggleState2
            toggleState3 shouldBe correctToggleState3
        }
    }
})

private class ToggleStateTestTestContext {
    val objectMapper = buildObjectMapper
    val correctJsonToggleState1 =
        """
           {"identitetsnummer":"01017012345","arbeidssoekerId":10001,"microfrontends":{}}
        """.trimIndent()
    val correctJsonToggleState2 =
        """
           {"identitetsnummer":"02017012345","arbeidssoekerId":10002,"microfrontends":{"aia-min-side":{"status":"UKJENT_VERDI","updated":"2025-01-01T13:37:00Z"}}}
        """.trimIndent()
    val correctJsonToggleState3 =
        """
           {"identitetsnummer":"03017012345","arbeidssoekerId":10003,"microfrontends":{"aia-min-side":{"status":"ENABLED","updated":"2025-01-01T13:37:00Z"},"aia-behovsvurdering":{"status":"DISABLED","updated":"2025-01-30T13:37:00Z"}}}
        """.trimIndent()
}