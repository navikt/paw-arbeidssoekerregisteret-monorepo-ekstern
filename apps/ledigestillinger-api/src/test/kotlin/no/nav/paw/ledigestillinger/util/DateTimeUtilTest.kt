package no.nav.paw.ledigestillinger.util

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class DateTimeUtilTest : FreeSpec({
    "Skal tolke uformaterte datoer" {
        "".fromUnformattedString() shouldBe null
        "whatever".fromUnformattedString() shouldBe null
        "01.01.2025".fromUnformattedString() shouldBe LocalDate.of(2025, 1, 1)
        "2025-02-02".fromUnformattedString() shouldBe LocalDate.of(2025, 2, 2)
        "2025-03-03T12:13:14".fromUnformattedString() shouldBe LocalDate.of(2025, 3, 3)
        "2025-04-04T13:14:15Z".fromUnformattedString() shouldBe LocalDate.of(2025, 4, 4)
    }
})