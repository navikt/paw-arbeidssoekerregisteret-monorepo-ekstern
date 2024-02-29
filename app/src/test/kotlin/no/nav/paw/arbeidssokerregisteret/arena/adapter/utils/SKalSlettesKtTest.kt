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
    "Topics.skalSlettes" - {
        val gjeldeneTid = Instant.now()
        "når ingen av feltene er satt skal den slettes" {
            val topicsJoin = TopicsJoin(null, null, null)
            topicsJoin.skalSlettes(gjeldeneTid) shouldBe true
        }
        "når bare periode er satt" - {
            "og den er aktiv" {
                val topicsJoin = topicsJoinMedPeriode()
                topicsJoin.skalSlettes(gjeldeneTid) shouldBe false
            }
            "og den er avsluttet" - {
                "skal den ikke slettes før tiden har utløpt" {
                    val topicsJoin = topicsJoinMedPeriode(
                        periodeAvsluttet = gjeldeneTid - 30.dager
                    )
                    topicsJoin.skalSlettes(
                        gjeldeneTid = gjeldeneTid,
                        tidsfristVentePaaOpplysninger = 31.dager
                    ) shouldBe false
                }
                "skal den slettes når tiden har utløpt" {
                    val topicsJoin = topicsJoinMedPeriode(
                        periodeAvsluttet = gjeldeneTid - 30.dager
                    )
                    topicsJoin.skalSlettes(
                        gjeldeneTid = gjeldeneTid,
                        tidsfristVentePaaOpplysninger = 29.dager
                    ) shouldBe true
                }
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
        "når opplysninger og periode er satt" - {
            "og profileringen ikke kommer innen fristen skal den slettes" {
                val topicsJoin = topicsJoinMedPeriode(
                    opplysningerSendtInn = gjeldeneTid - 2.dager
                )
                topicsJoin.skalSlettes(gjeldeneTid = gjeldeneTid, tidsfristVentePaaProfilering = 1.dager) shouldBe true
            }
            "og fristen for å vente på profileringen ikke er utløpt skal den ikke slettes" {
                val topicsJoin = topicsJoinMedPeriode(
                    opplysningerSendtInn = gjeldeneTid - 2.dager
                )
                topicsJoin.skalSlettes(gjeldeneTid, tidsfristVentePaaProfilering = 3.dager) shouldBe false
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
    opplysningerSendtInn: Instant? = null
): TopicsJoin = TopicsJoin(
    Periode(
        UUID.randomUUID(),
        "",
        metadata(Instant.now().minus(Duration.ofDays(30))),
        periodeAvsluttet?.let(::metadata)
    ),
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