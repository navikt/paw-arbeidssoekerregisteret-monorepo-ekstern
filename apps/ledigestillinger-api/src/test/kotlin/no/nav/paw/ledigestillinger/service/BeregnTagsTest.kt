package no.nav.paw.ledigestillinger.service

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.pam.stilling.ext.avro.Property
import no.nav.paw.ledigestillinger.model.asTags
import no.nav.paw.ledigestillinger.test.TestData.baseAd
import no.naw.paw.ledigestillinger.model.Tag

class BeregnTagsTest : FreeSpec({

    "beregnTags" - {
        "returnerer tom mengde når source ikke er DIR og properties er tom" {
            baseAd().asTags() shouldBe emptySet()
        }

        "returnerer tom mengde når source er DIR men direktemeldtStillingskategori mangler" {
            val ad = baseAd().apply { source = "DIR" }
            ad.asTags() shouldBe emptySet()
        }

        "returnerer tom mengde når source er DIR men direktemeldtStillingskategori ikke er STILLING" {
            val ad = baseAd().apply {
                source = "DIR"
                properties = listOf(Property("direktemeldtStillingskategori", "ANNET"))
            }
            ad.asTags() shouldBe emptySet()
        }

        "returnerer DIREKTEMELDT_V1 når source er DIR og direktemeldtStillingskategori er STILLING" {
            val ad = baseAd().apply {
                source = "DIR"
                properties = listOf(Property("direktemeldtStillingskategori", "STILLING"))
            }
            ad.asTags() shouldBe setOf(Tag.DIREKTEMELDT_V1)
        }

        "returnerer DIREKTEMELDT_V1 når source er dir(lowercase) og direktemeldtStillingskategori er stilling(lowercase)" {
            val ad = baseAd().apply {
                source = "dir"
                properties = listOf(Property("direktemeldtStillingskategori", "stilling"))
            }
            ad.asTags() shouldBe setOf(Tag.DIREKTEMELDT_V1)
        }

        "er case-insensitiv for source og direktemeldtStillingskategori" {
            val ad = baseAd().apply {
                source = "dir"
                properties = listOf(Property("DIREKTEMELDTSTILLINGSKATEGORI", "stilling"))
            }
            ad.asTags() shouldBe setOf(Tag.DIREKTEMELDT_V1)
        }
    }
})
