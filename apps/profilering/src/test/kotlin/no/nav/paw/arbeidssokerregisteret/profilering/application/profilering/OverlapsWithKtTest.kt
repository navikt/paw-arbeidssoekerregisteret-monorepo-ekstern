package no.nav.paw.arbeidssokerregisteret.profilering.application.profilering

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class OverlapsWithKtTest : FreeSpec({
    "overlapsWith" - {
        testCases.forEach { (a, b, overlaps) ->
            val result = a overlapsWith b
            "$a and $b should ${if (overlaps) "" else "not "}overlap" {
                result shouldBe overlaps
            }
        }
    }
})

val testCases = listOf(
    TestCase(
        a = LocalDate.of(2022, 1, 1).rangeUntil(LocalDate.of(2022, 1, 10)),
        b = LocalDate.of(2022, 1, 5).rangeUntil(LocalDate.of(2022, 1, 15)),
        overlaps = true
    ),
    TestCase(
        a = LocalDate.of(2022, 1, 1).rangeUntil(LocalDate.of(2022, 1, 10)),
        b = LocalDate.of(2022, 1, 10).rangeUntil(LocalDate.of(2022, 1, 20)),
        overlaps = false
    ),
    TestCase(
        a = LocalDate.of(2022, 1, 1).rangeUntil(LocalDate.of(2022, 1, 10)),
        b = LocalDate.of(2022, 2, 1).rangeUntil(LocalDate.of(2022, 2, 10)),
        overlaps = false
    ),
    TestCase(
        a = LocalDate.of(2022, 1, 1).rangeUntil(LocalDate.of(2022, 1, 10)),
        b = LocalDate.of(2021, 12, 1).rangeUntil(LocalDate.of(2022, 1, 1)),
        overlaps = false
    ),
    TestCase(
        a = LocalDate.of(2022, 1, 1).rangeUntil(LocalDate.of(2024, 1, 10)),
        b = LocalDate.of(2023, 12, 1).rangeUntil(LocalDate.of(2023, 1, 1)),
        overlaps = true
    ),
    TestCase(
        a = LocalDate.of(2023, 12, 1).rangeUntil(LocalDate.of(2023, 1, 1)),
        b = LocalDate.of(2022, 1, 1).rangeUntil(LocalDate.of(2024, 1, 10)),
        overlaps = true
    ),
    TestCase(
        a = LocalDate.of(2023, 1, 1).rangeUntil(LocalDate.of(2023, 1, 10)),
        b = LocalDate.of(2023, 1, 1).rangeUntil(LocalDate.of(2023, 1, 10)),
        overlaps = true
    ),
    TestCase(
        a = LocalDate.of(2023, 1, 1).rangeUntil(LocalDate.of(2023, 1, 10)),
        b = LocalDate.of(2023, 1, 9).rangeUntil(LocalDate.of(2023, 1, 11)),
        overlaps = true
    )
)

data class TestCase(
    val a: OpenEndRange<LocalDate>,
    val b: OpenEndRange<LocalDate>,
    val overlaps: Boolean
)
