package no.naw.paw.ledigestillinger

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import java.time.Duration
import java.time.Instant

data class B (
    val enBoolean: Boolean,
    val enInt: Int,
    val enDuration: Duration
)

data class A(
    val enStreng: String,
    val enInstant: Instant,
    val liste: List<String>,
    val enAnnenListe: List<Int>,
    val enB: B,
    val enNullableB: B?
)

class ObjectMapperTest: FreeSpec({
    "Verifiser at ikke noe går tapt (feks nano-sekunder i Duration og Instant)" {
        val duration = Duration.ofNanos(658743657843L)
        val a = A(
            enStreng = "hei",
            enInstant = Instant.EPOCH + duration,
            liste = listOf("a", "b", "c"),
            enAnnenListe = listOf(1, 2, 3),
            enB = B(
                enBoolean = true,
                enInt = 42,
                enDuration = duration
            ),
            enNullableB = null
        )

        val json = ledigeStillingerApiObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(a)
        val a2 = ledigeStillingerApiObjectMapper.readValue(json, A::class.java)
        println(json)
        println(a2)
        a2 shouldBe a
    }
})