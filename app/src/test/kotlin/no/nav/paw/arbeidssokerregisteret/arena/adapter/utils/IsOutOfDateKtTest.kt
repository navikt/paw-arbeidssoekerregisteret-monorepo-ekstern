package no.nav.paw.arbeidssokerregisteret.arena.adapter.utils

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssokerregisteret.arena.helpers.v3.TopicsJoin
import no.nav.paw.arbeidssokerregisteret.arena.v1.*
import no.nav.paw.arbeidssokerregisteret.arena.v1.Metadata as ArenaMetadata
import no.nav.paw.arbeidssokerregisteret.arena.v3.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.arena.v3.Utdanning
import java.time.Duration
import java.time.Instant
import java.util.*

class IsOutOfDateKtTest : FreeSpec({
    "isOutOfDate" - {
        "should return true" - {
            "when avsluttet is null" - {
                "and tidspunktForOpplysninger is null" {
                    TopicsJoin().isOutOfDate(Instant.now()) shouldBe true
                }
                "and tidspunktForOpplysninger is out of date" {
                    topicsJoinMedPeriode(
                        opplysningerSendtInn = Instant.now().minus(Duration.ofDays(100))
                    ).isOutOfDate(Instant.now()) shouldBe true
                }
            }
            "when avsluttet is out of date" - {
                topicsJoinMedPeriode(
                    periodeAvslutet = Instant.now().minus(Duration.ofDays(100))
                ).isOutOfDate(Instant.now()) shouldBe true
            }
        }
        "should return false" - {
            "when avsluttet is null" - {
                "and tidspunktForOpplysninger is not out of date" {
                    topicsJoinMedPeriode(
                        opplysningerSendtInn = Instant.now().minus(Duration.ofMinutes(1))
                    ).isOutOfDate(Instant.now()) shouldBe false
                }
            }
            "when avsluttet is not out of date" {
                topicsJoinMedPeriode(
                    periodeAvslutet = Instant.now().minus(Duration.ofMinutes(1))
                ).isOutOfDate(Instant.now()) shouldBe false
            }
        }
    }
})

fun metadata(timestamp: Instant): ArenaMetadata {
    return ArenaMetadata(
        timestamp,
        Bruker(
            BrukerType.SYSTEM,
            ""
        ),
        "",
        ""
    )
}

fun topicsJoinMedPeriode(
    periodeAvslutet: Instant? = null,
    opplysningerSendtInn: Instant? = null
): TopicsJoin = TopicsJoin(
    Periode(
        UUID.randomUUID(),
        "",
        metadata(Instant.now().minus(Duration.ofDays(30))),
        periodeAvslutet?.let(::metadata)
    ),
    null,
    opplysningerSendtInn?.let {
        OpplysningerOmArbeidssoeker(
            UUID.randomUUID(),
            UUID.randomUUID(),
            metadata(it),
            Utdanning("9", JaNeiVetIkke.JA, JaNeiVetIkke.JA),
            Helse(JaNeiVetIkke.NEI),
            Arbeidserfaring(JaNeiVetIkke.NEI),
            Jobbsituasjon(emptyList()),
            Annet(JaNeiVetIkke.JA),
        )
    }
)

fun topicsJoinUtenPeriode(
    opplysningerSendtInn: Instant? = null
): TopicsJoin = TopicsJoin(
    null,
    null,
    opplysningerSendtInn?.let {
        OpplysningerOmArbeidssoeker(
            UUID.randomUUID(),
            UUID.randomUUID(),
            metadata(it),
            Utdanning("9", JaNeiVetIkke.JA, JaNeiVetIkke.JA),
            Helse(JaNeiVetIkke.NEI),
            Arbeidserfaring(JaNeiVetIkke.NEI),
            Jobbsituasjon(emptyList()),
            Annet(JaNeiVetIkke.JA),
        )
    }
)