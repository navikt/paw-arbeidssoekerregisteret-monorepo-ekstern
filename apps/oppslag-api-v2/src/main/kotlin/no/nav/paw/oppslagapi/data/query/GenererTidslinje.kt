package no.nav.paw.oppslagapi.data.query

import io.opentelemetry.api.common.AttributeKey.booleanKey
import io.opentelemetry.api.common.AttributeKey.longKey
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import no.nav.paw.oppslagapi.data.Row
import no.nav.paw.oppslagapi.data.bekreftelsemelding_v1
import no.nav.paw.oppslagapi.data.periode_avsluttet_v1
import no.nav.paw.oppslagapi.data.periode_startet_v1
import no.nav.paw.oppslagapi.model.v2.Hendelse
import no.nav.paw.oppslagapi.model.v2.HendelseType
import no.nav.paw.oppslagapi.model.v2.Tidslinje
import java.time.Duration
import java.time.Duration.between
import java.time.Instant
import java.util.*

fun genererTidslinje(rader: List<Pair<UUID, List<Row<Any>>>>): List<Tidslinje> {
    val filtrertData = rader.filter { (_, rader) -> rader.isNotEmpty() }
    val startMap = filtrertData
        .associate { (periodeId, rader) ->
            val førsteRegistrerte = rader.minByOrNull { it.timestamp }
            val tidsDiff = between(førsteRegistrerte?.timestamp ?: Instant.EPOCH, Instant.now())
            val startHendelse = rader.firstOrNull { it.type == periode_startet_v1 }
            if (startHendelse != null) {
                periodeId to startHendelse
            } else {
                Span.current().addEvent(
                    "periode_uten_start", Attributes.of(
                        stringKey("foerste_registrerte_hendelse_type"), førsteRegistrerte?.type ?: "ukjent",
                        longKey("ms_siden_foerste_hendelse"), tidsDiff.toMillis(),
                        booleanKey("under_ett_sekund_diff"), tidsDiff < Duration.ofSeconds(1)
                    )
                )
                throw PeriodeUtenStartHendelseException(
                    hendelseType = førsteRegistrerte?.type ?: "ukjent",
                    hendelseAlder = tidsDiff
                )
            }
        }
    val avsluttetMap = filtrertData.associate { (periodeId, rader) ->
        periodeId to rader.firstOrNull { it.type == periode_avsluttet_v1 }
    }
    val bekreftelser: Map<UUID, List<Hendelse>> = filtrertData.mapNotNull { (periodeId, rader) ->
        genererTidslinje(periodeId, rader)?.let { periodeId to it.bekreftelser }
    }.associate { (periodeId, bekreftelserMedMetadata) ->
        periodeId to bekreftelserMedMetadata.map { bekreftelse ->
            Hendelse(
                hendelseType = HendelseType.bekreftelse_v1,
                tidspunkt = bekreftelse.bekreftelse!!.svar.sendtInnAv.tidspunkt,
                bekreftelseV1 = bekreftelse
            )
        }
    }

    val andreHendelser: Map<UUID, List<Hendelse>> = filtrertData.mapNotNull { (periodeId, rader) ->
        periodeId to rader.filter { it.type != bekreftelsemelding_v1 }
    }.associate { (periodeId, rader) ->
        periodeId to rader.map { rad ->
            mapIkkeBekreftelseRaderTilHendelser(rad)
        }
    }

    val apiTidslinjer = andreHendelser.map { (periodeId, hendelser) ->
        val start = startMap[periodeId]!!
        val avsluttet = avsluttetMap[periodeId]
        Tidslinje(
            periodeId = periodeId,
            identitetsnummer = start.identitetsnummer!!,
            startet = start.timestamp,
            hendelser = (hendelser + (bekreftelser[periodeId] ?: emptyList())).sortedByDescending { it.tidspunkt },
            avsluttet = avsluttet?.timestamp
        )
    }.sortedByDescending { it.startet }
    return apiTidslinjer
}