package no.nav.paw.ledigestillinger.model

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.naw.paw.ledigestillinger.model.Paging

class DaoMapperTest : FreeSpec({
    "Skal teste paginering" {
        Paging(-1, 10).offset() shouldBe 0
        Paging(0, 10).offset() shouldBe 0
        Paging(1, 10).offset() shouldBe 0
        Paging(2, 10).offset() shouldBe 10
        Paging(3, 10).offset() shouldBe 20
        Paging(4, 10).offset() shouldBe 30
        Paging(5, 10).offset() shouldBe 40
        Paging(6, 10).offset() shouldBe 50
        Paging(7, 10).offset() shouldBe 60
        Paging(8, 10).offset() shouldBe 70
        Paging(9, 10).offset() shouldBe 80
        Paging(10, 10).offset() shouldBe 90

        Paging(1, -1).size() shouldBe 10
        Paging(1, 0).size() shouldBe 10
        Paging(1, 1).size() shouldBe 1
        Paging(1, 2).size() shouldBe 2
        Paging(1, 3).size() shouldBe 3
        Paging(1, 4).size() shouldBe 4
        Paging(1, 5).size() shouldBe 5
        Paging(1, 6).size() shouldBe 6
        Paging(1, 7).size() shouldBe 7
        Paging(1, 8).size() shouldBe 8
        Paging(1, 9).size() shouldBe 9
        Paging(1, 10).size() shouldBe 10
    }

    "Skal teste styrk normalisering" {
        "1.23456".asNormalisertKode() shouldBe "1"
        "12.3456".asNormalisertKode() shouldBe "12"
        "123.456".asNormalisertKode() shouldBe "123"
        "1234.56".asNormalisertKode() shouldBe "1234"
        "12345.6".asNormalisertKode() shouldBe "12345"
        "123456".asNormalisertKode() shouldBe "123456"
        "Whatever".asNormalisertKode() shouldBe "Whatever"
    }
})