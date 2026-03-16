package no.naw.paw.minestillinger

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.testcontainers.shaded.org.bouncycastle.math.raw.Interleave

class InterleaveTest : FreeSpec({

    "interleave" {
        val a: List<String> = listOf("a_1", "a_2", "a_3")
        val b: List<String> = listOf("b_1", "b_2", "b_3", "b_4", "b_5", "b_6", "b_7")
        val c: List<String> = listOf("c_1", "c_2", "c_3", "c_4", "c_5")
        val d: List<String> = emptyList()
        val interleaved = listOf(a, b, c, d).interleave()
        interleaved shouldBe listOf(
            "a_1", "b_1", "c_1",
            "a_2", "b_2", "c_2",
            "a_3", "b_3", "c_3",
            "b_4", "c_4",
            "b_5", "c_5",
            "b_6",
            "b_7"
        )
    }
})
