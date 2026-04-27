package no.nav.paw.ledigestillinger.service

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.pam.stilling.ext.avro.Property
import no.nav.paw.ledigestillinger.model.asTags
import no.nav.paw.ledigestillinger.test.TestData
import no.naw.paw.ledigestillinger.model.Tag
import no.naw.paw.ledigestillinger.model.TekniskTag

class UtledTagsTest : FreeSpec({

    "Utled tags for direktemeldte stillinger" - {
        "returnerer tom mengde når source ikke er DIR og properties er tom" {
            val (_, ad) = TestData.ad()
            val (tags, tekniskeTags) = ad.asTags()
            tags shouldBe emptySet()
            tekniskeTags shouldBe emptySet()
        }

        "returnerer tom mengde når source er DIR men direktemeldtStillingskategori mangler" {
            val (_, ad) = TestData.ad(source = "DIR")
            val (tags, tekniskeTags) = ad.asTags()
            tags shouldBe emptySet()
            tekniskeTags shouldBe emptySet()
        }

        "returnerer tom mengde når source er DIR men direktemeldtStillingskategori ikke er STILLING" {
            val (_, ad) = TestData.ad(
                source = "DIR",
                properties = listOf(Property("direktemeldtStillingskategori", "ANNET"))
            )
            val (tags, tekniskeTags) = ad.asTags()
            tags shouldBe emptySet()
            tekniskeTags shouldBe emptySet()
        }

        "returnerer DIREKTEMELDT_V1 når source er DIR og direktemeldtStillingskategori er STILLING" {
            val (_, ad) = TestData.ad(
                source = "DIR",
                properties = listOf(Property("direktemeldtStillingskategori", "STILLING"))
            )
            val (tags, tekniskeTags) = ad.asTags()
            tags shouldBe setOf(Tag.DIREKTEMELDT_V1)
            tekniskeTags shouldBe emptySet()
        }

        "returnerer DIREKTEMELDT_V1 når source er dir(lowercase) og direktemeldtStillingskategori er stilling(lowercase)" {
            val (_, ad) = TestData.ad(
                source = "dir",
                properties = listOf(Property("direktemeldtStillingskategori", "stilling"))
            )
            val (tags, tekniskeTags) = ad.asTags()
            tags shouldBe setOf(Tag.DIREKTEMELDT_V1)
            tekniskeTags shouldBe emptySet()
        }

        "er case-insensitiv for source og direktemeldtStillingskategori" {
            val (_, ad) = TestData.ad(
                source = "dir",
                properties = listOf(Property("DIREKTEMELDTSTILLINGSKATEGORI", "stilling"))
            )
            val (tags, tekniskeTags) = ad.asTags()
            tags shouldBe setOf(Tag.DIREKTEMELDT_V1)
            tekniskeTags shouldBe emptySet()
        }
    }

    "Utled tags for krav til stillinger" - {
        "Skal utlede ingen tags ved tom eksperimentelleProperties" {
            val (_, ad) = TestData.ad()
            val (tags, tekniskeTags) = ad.asTags()
            tags shouldBe emptySet()
            tekniskeTags shouldBe emptySet()
        }

        "Skal utlede ingen tags ved ingen verdi" {
            val (_, ad) = TestData.ad(
                eksperimentelleProperties = listOf(
                    Property("experience", null),
                    Property("education", null),
                    Property("needDriversLicense", null),
                )
            )
            val (tags, tekniskeTags) = ad.asTags()
            tags shouldBe emptySet()
            tekniskeTags shouldBe emptySet()
        }

        "Skal utlede tekniske tags ved ukjente verdier" {
            val (_, ad) = TestData.ad(
                eksperimentelleProperties = listOf(
                    Property("experience", "Whetever"),
                    Property("education", "Whetever"),
                )
            )
            val (tags, tekniskeTags) = ad.asTags()
            tags shouldBe emptySet()
            tekniskeTags shouldBe setOf(
                TekniskTag.UKJENT_KRAV_TIL_ARBEIDSERFARING_V1,
                TekniskTag.UKJENT_KRAV_TIL_UTDANNING_V1,
            )
        }

        "Skal utlede tags ved kjente verdier 1" {
            val (_, ad) = TestData.ad(
                eksperimentelleProperties = listOf(
                    Property("experience", "[\"Ingen\""),
                    Property("education", "[\"Master\"]"),
                    Property("needDriversLicense", "Whetever"),
                )
            )
            val (tags, tekniskeTags) = ad.asTags()
            tags shouldBe setOf(
                Tag.INGEN_KRAV_TIL_ARBEIDSERFARING_V1,
                Tag.HAR_KRAV_TIL_UTDANNING_V1,
            )
            tekniskeTags shouldBe setOf(
                TekniskTag.UKJENT_KRAV_TIL_FOERERKORT_V1
            )
        }

        "Skal utlede tags ved kjente verdier 2" {
            val (_, ad) = TestData.ad(
                eksperimentelleProperties = listOf(
                    Property("experience", "[\"Noe\",\"Whetever\""),
                    Property("education", "[\"Fagskole\",\"Fagbrev\"]"),
                    Property("needDriversLicense", "true"),
                )
            )
            val (tags, tekniskeTags) = ad.asTags()
            tags shouldBe setOf(
                Tag.HAR_KRAV_TIL_ARBEIDSERFARING_V1,
                Tag.HAR_KRAV_TIL_UTDANNING_V1,
                Tag.HAR_KRAV_TIL_FOERERKORT_V1
            )
            tekniskeTags shouldBe emptySet()
        }

        "Skal utlede tags ved kjente verdier 3" {
            val (_, ad) = TestData.ad(
                eksperimentelleProperties = listOf(
                    Property("experience", "[\"Mye\""),
                    Property("needDriversLicense", "false"),
                )
            )
            val (tags, tekniskeTags) = ad.asTags()
            tags shouldBe setOf(
                Tag.HAR_KRAV_TIL_ARBEIDSERFARING_V1,
                Tag.INGEN_KRAV_TIL_FOERERKORT_V1
            )
            tekniskeTags shouldBe emptySet()
        }
    }
})
