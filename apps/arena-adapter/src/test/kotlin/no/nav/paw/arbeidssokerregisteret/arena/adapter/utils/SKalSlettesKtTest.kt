package no.nav.paw.arbeidssokerregisteret.arena.adapter.utils

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssokerregisteret.arena.helpers.v4.TopicsJoin
import no.nav.paw.arbeidssokerregisteret.arena.v1.*
import no.nav.paw.arbeidssokerregisteret.arena.v1.Metadata as ArenaMetadata
import no.nav.paw.arbeidssokerregisteret.arena.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.arena.v4.Utdanning
import no.nav.paw.arbeidssokerregisteret.arena.v2.Annet
import java.time.Duration
import java.time.Instant
import java.util.*

class SKalSlettesKtTest : FreeSpec({
    "TopicsJoin.skalSlettes" - {
        val gjeldeneTid = Instant.now()
        "når ingen av feltene er satt skal den slettes" {
            val topicsJoin = TopicsJoin(null, null, null)
            topicsJoin.skalSlettes(gjeldeneTid) shouldBe true
        }
        "skal aldri slettes når perioden ikke av avsluttet" - {
            "selv om opplysninger er mottatt" {
                val topicsJoin = topicsJoinMedPeriode(
                    opplysningerSendtInn = gjeldeneTid - 2.dager,
                    periodeAvsluttet = null
                )
                topicsJoin.skalSlettes(gjeldeneTid = gjeldeneTid, tidsfristVentePaaProfilering = 1.dager) shouldBe false
            }
            "selv om opplysninger og profilering er mottatt" {
                val topicsJoin = topicsJoinMedPeriode(
                    opplysningerSendtInn = gjeldeneTid - 2.dager,
                    profileringsSendtInn = gjeldeneTid - 1.dager,

                    )
                topicsJoin.skalSlettes(gjeldeneTid, tidsfristVentePaaProfilering = 3.dager) shouldBe false
            }
            "selv om ingen andre meldinger er mottatt" {
                val topicsJoin = topicsJoinMedPeriode()
                topicsJoin.skalSlettes(gjeldeneTid, tidsfristVentePaaProfilering = 3.dager) shouldBe false
            }
        }
        "når perioden er avsluttet skal den bare slettes dersom" - {
            "opplysninger ikke ankommer innen tidsfristen" {
                val topicsJoin = topicsJoinMedPeriode(
                    periodeAvsluttet= gjeldeneTid - 4.dager
                )
                topicsJoin.skalSlettes(gjeldeneTid = gjeldeneTid, tidsfristVentePaaOpplysninger = 3.dager) shouldBe true
                topicsJoin.skalSlettes(gjeldeneTid = gjeldeneTid, tidsfristVentePaaOpplysninger = 5.dager) shouldBe false
            }
            "profilering ikke ankommer innen tidsfristen" {
                val topicsJoin = topicsJoinMedPeriode(
                    periodeAvsluttet = gjeldeneTid - 4.dager,
                    opplysningerSendtInn = gjeldeneTid - 3.dager
                )
                topicsJoin.skalSlettes(gjeldeneTid, tidsfristVentePaaProfilering = 2.dager) shouldBe true
                topicsJoin.skalSlettes(gjeldeneTid, tidsfristVentePaaProfilering = 4.dager) shouldBe false
            }
        }
        "når bare opplysninger er satt" - {
            "og perioden ikke kommer innen fristen skal den slettes" {
                val topicsJoin = topicsJoinUtenPeriode(
                    opplysningerSendtInn = gjeldeneTid - 2.dager
                )
                topicsJoin.skalSlettes(gjeldeneTid = gjeldeneTid, tidsfristVentePaaPeriode = 1.dager) shouldBe true
            }
            "og fristen for å påvente perioden ikke er utløpt skal den ikke slettes" {
                val topicsJoin = topicsJoinUtenPeriode(
                    opplysningerSendtInn = gjeldeneTid - 2.dager
                )
                topicsJoin.skalSlettes(gjeldeneTid, tidsfristVentePaaPeriode = 3.dager) shouldBe false
            }
        }
        "når bare profilering er mottatt skal den slettes" {
            val topicsJoin = TopicsJoin(
                null,
                Profilering(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    metadata(gjeldeneTid - 2.dager),
                    ProfilertTil.ANTATT_GODE_MULIGHETER,
                    true,
                    42
                ),
                null
            )
            topicsJoin.skalSlettes(gjeldeneTid = gjeldeneTid) shouldBe true
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
    periodeAvsluttet: Instant? = null,
    opplysningerSendtInn: Instant? = null,
    profileringsSendtInn: Instant? = null
): TopicsJoin = TopicsJoin(
    Periode(
        UUID.randomUUID(),
        "",
        metadata(Instant.now().minus(Duration.ofDays(30))),
        periodeAvsluttet?.let(::metadata)
    ),
    profileringsSendtInn?.let {
        Profilering(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            metadata(it),
            ProfilertTil.ANTATT_GODE_MULIGHETER,
            true,
            42
        )
    },
    opplysningerSendtInn?.let {
        OpplysningerOmArbeidssoeker(
            UUID.randomUUID(),
            UUID.randomUUID(),
            metadata(it),
            Utdanning("9", JaNeiVetIkke.JA, JaNeiVetIkke.JA),
            Helse(JaNeiVetIkke.NEI),
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
            Jobbsituasjon(emptyList()),
            Annet(JaNeiVetIkke.JA),
        )
    }
)

val Int.dager: Duration get() = Duration.ofDays(this.toLong())