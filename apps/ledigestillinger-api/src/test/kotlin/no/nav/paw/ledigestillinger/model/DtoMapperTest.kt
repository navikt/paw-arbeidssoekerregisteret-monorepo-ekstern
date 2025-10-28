package no.nav.paw.ledigestillinger.model

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.naw.paw.ledigestillinger.model.Frist
import no.naw.paw.ledigestillinger.model.FristType
import java.time.LocalDate

class DtoMapperTest : FreeSpec({
    "Skal teste tolking av frist" {
        "".asFrist() shouldBe Frist(type = FristType.UKJENT, verdi = "")
        "whatever".asFrist() shouldBe Frist(type = FristType.UKJENT, verdi = "whatever")
        "blah snart yolo".asFrist() shouldBe Frist(type = FristType.SNAREST, verdi = "blah snart yolo")
        "blah snarest yolo".asFrist() shouldBe Frist(type = FristType.SNAREST, verdi = "blah snarest yolo")
        "blah fortløpende yolo".asFrist() shouldBe Frist(type = FristType.FORTLOEPENDE, verdi = "blah fortløpende yolo")
        "1234".asFrist() shouldBe Frist(
            type = FristType.UKJENT,
            verdi = "1234",
        )
        "01.01.2025".asFrist() shouldBe Frist(
            type = FristType.DATO,
            verdi = "01.01.2025",
            dato = LocalDate.of(2025, 1, 1)
        )
        "2025-02-02".asFrist() shouldBe Frist(
            type = FristType.DATO,
            verdi = "2025-02-02",
            dato = LocalDate.of(2025, 2, 2)
        )
        "2025-03-03T12:13:14".asFrist() shouldBe Frist(
            type = FristType.DATO,
            verdi = "2025-03-03T12:13:14",
            dato = LocalDate.of(2025, 3, 3)
        )
        "2025-04-04T13:14:15Z".asFrist() shouldBe Frist(
            type = FristType.DATO,
            verdi = "2025-04-04T13:14:15Z",
            dato = LocalDate.of(2025, 4, 4)
        )
    }
})