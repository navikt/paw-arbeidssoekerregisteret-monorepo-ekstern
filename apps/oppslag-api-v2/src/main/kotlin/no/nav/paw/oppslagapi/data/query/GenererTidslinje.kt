package no.nav.paw.oppslagapi.data.query

import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Hendelse
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.HendelseType
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Tidslinje
import no.nav.paw.oppslagapi.data.Row
import no.nav.paw.oppslagapi.data.bekreftelsemelding_v1
import no.nav.paw.oppslagapi.data.periode_avsluttet_v1
import no.nav.paw.oppslagapi.data.periode_startet_v1
import java.util.*

fun genererTidslinje(rader: List<Pair<UUID, List<Row<out Any>>>>): List<Tidslinje> {
    val startMap = rader.associate { (periodeId, rader) ->
        periodeId to (rader.firstOrNull { it.type == periode_startet_v1 }
            ?: throw IllegalStateException("Periode uten start hendelse: $periodeId")
                )
    }
    val avsluttetMap = rader.associate { (periodeId, rader) ->
        periodeId to rader.firstOrNull { it.type == periode_avsluttet_v1 }
    }
    val bekreftelser: Map<UUID, List<Hendelse>> = rader.mapNotNull { (periodeId, rader) ->
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

    val andreHendelser: Map<UUID, List<Hendelse>> = rader.mapNotNull { (periodeId, rader) ->
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
            hendelser = (hendelser + (bekreftelser[periodeId] ?: emptyList())).sortedBy { it.tidspunkt },
            avsluttet = avsluttet?.timestamp
        )
    }.sortedBy { it.startet }
    return apiTidslinjer
}