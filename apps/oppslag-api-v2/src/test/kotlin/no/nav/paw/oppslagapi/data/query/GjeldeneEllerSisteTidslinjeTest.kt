package no.nav.paw.oppslagapi.data.query

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.types.shouldBeSameInstanceAs
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Tidslinje
import java.time.Instant
import java.util.UUID

class GjeldeneEllerSisteTidslinjeTest: FreeSpec({
    "Ved 3 inaktive perioder skal siste avsluttede returneres" {
        val tidslinjer = listOf(
            Tidslinje(
                startet = Instant.parse("2020-01-01T00:00:00Z"),
                avsluttet = Instant.parse("2020-02-01T00:00:00Z"),
                hendelser = emptyList(),
                identitetsnummer = "12345678901",
                periodeId = UUID.randomUUID()
            ),
            Tidslinje(
                startet = Instant.parse("2021-01-01T00:00:00Z"),
                avsluttet = Instant.parse("2022-02-01T00:00:00Z"),
                hendelser = emptyList(),
                identitetsnummer = "12345678901",
                periodeId = UUID.randomUUID()
            ),
            Tidslinje(
                startet = Instant.parse("2021-01-01T00:00:00Z"),
                avsluttet = Instant.parse("2021-02-01T00:00:00Z"),
                hendelser = emptyList(),
                identitetsnummer = "12345678901",
                periodeId = UUID.randomUUID()
            )
        )
        tidslinjer.gjeldeneEllerSisteTidslinje() shouldBeSameInstanceAs tidslinjer[1]
    }
    "Ved en aktive og 2 inaktive perioder skal aktive periode returneres" {
        val tidslinjer = listOf(
            Tidslinje(
                startet = Instant.parse("2020-01-01T00:00:00Z"),
                avsluttet = Instant.parse("2020-02-01T00:00:00Z"),
                hendelser = emptyList(),
                identitetsnummer = "12345678901",
                periodeId = UUID.randomUUID()
            ),
            Tidslinje(
                startet = Instant.parse("2021-01-01T00:00:00Z"),
                avsluttet = Instant.parse("2022-02-01T00:00:00Z"),
                hendelser = emptyList(),
                identitetsnummer = "12345678901",
                periodeId = UUID.randomUUID()
            ),
            Tidslinje(
                startet = Instant.parse("2019-01-01T00:00:00Z"),
                avsluttet = null,
                hendelser = emptyList(),
                identitetsnummer = "12345678901",
                periodeId = UUID.randomUUID()
            )
        )
        tidslinjer.gjeldeneEllerSisteTidslinje() shouldBeSameInstanceAs tidslinjer[2]
        tidslinjer.gjeldeneEllerSisteTidslinje()?.avsluttet.shouldBeNull()
    }
    "Ved 3 aktive perioder skal eldste periode returneres" {
        val tidslinjer = listOf(
            Tidslinje(
                startet = Instant.parse("2018-01-01T00:00:00Z"),
                avsluttet = null,
                hendelser = emptyList(),
                identitetsnummer = "12345678901",
                periodeId = UUID.randomUUID()
            ),
            Tidslinje(
                startet = Instant.parse("2021-01-01T00:00:00Z"),
                avsluttet = null,
                hendelser = emptyList(),
                identitetsnummer = "12345678901",
                periodeId = UUID.randomUUID()
            ),
            Tidslinje(
                startet = Instant.parse("2019-01-01T00:00:00Z"),
                avsluttet = null,
                hendelser = emptyList(),
                identitetsnummer = "12345678901",
                periodeId = UUID.randomUUID()
            )
        )
        tidslinjer.gjeldeneEllerSisteTidslinje() shouldBeSameInstanceAs tidslinjer[0]
        tidslinjer.gjeldeneEllerSisteTidslinje()?.avsluttet.shouldBeNull()
    }
})