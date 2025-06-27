package no.nav.paw.oppslagapi.data.query

import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.ApiV2BekreftelserPostRequest
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.BekreftelserResponse
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Hendelse
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.HendelseType
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Metadata
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.PaaVegneAvStart
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.PaaVegneAvStopp
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Profilering
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Tidslinje
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.TidslinjeResponse
import no.nav.paw.error.model.Data
import no.nav.paw.error.model.Response
import no.nav.paw.error.model.getOrThrow
import no.nav.paw.model.Identitetsnummer
import no.nav.paw.oppslagapi.AutorisasjonsTjeneste
import no.nav.paw.oppslagapi.data.Row
import no.nav.paw.oppslagapi.data.bekreftelsemelding_v1
import no.nav.paw.oppslagapi.data.opplysninger_om_arbeidssoeker_v4
import no.nav.paw.oppslagapi.data.pa_vegne_av_start_v1
import no.nav.paw.oppslagapi.data.pa_vegne_av_stopp_v1
import no.nav.paw.oppslagapi.data.periode_avsluttet_v1
import no.nav.paw.oppslagapi.data.periode_startet_v1
import no.nav.paw.oppslagapi.data.profilering_v1
import no.nav.paw.security.authentication.model.Bruker
import java.util.UUID

class ApplicationQueryLogic(
    private val autorisasjonsTjeneste: AutorisasjonsTjeneste,
    private val databaseQuerySupport: DatabaseQeurySupport
) {

    suspend fun hentBekreftelser(
        bruker: Bruker<out Any>,
        request: ApiV2BekreftelserPostRequest
    ): Response<BekreftelserResponse> {
        if (request.perioder.isEmpty()) {
            return Data(BekreftelserResponse(emptyList()))
        }
        val tidslinjer = request.perioder.mapNotNull { periodeId ->
            val rader = databaseQuerySupport.hentRaderForPeriode(periodeId)
            genererTidslinje(periodeId, rader)
        }
        val identerOensketInfoOm = tidslinjer.map { it.identitetsnummer }.toSet()
        return autorisasjonsTjeneste.autoriser(
            handling = "Hent bekreftelser for arbeidssøkerperiode",
            bruker = bruker,
            oenskerTilgangTil = identerOensketInfoOm.map(::Identitetsnummer),
        ) {
            BekreftelserResponse(
                bekreftelser = tidslinjer.flatMap { it.bekreftelser }
            )
        }
    }

    suspend fun lagTidslinjer(
        bruker: Bruker<out Any>,
        request: ApiV2BekreftelserPostRequest
    ): Response<TidslinjeResponse> {
        if (request.perioder.isEmpty()) {
            return Data(TidslinjeResponse(emptyList()))
        }
        val rader = request.perioder.mapNotNull { periodeId ->
            periodeId to databaseQuerySupport.hentRaderForPeriode(periodeId)
        }
        return autorisasjonsTjeneste.autoriser(
            handling = "Lag tidslinjer for arbeidssøkerperioder",
            bruker = bruker,
            oenskerTilgangTil = rader
                .flatMap { (_, rader) -> rader.mapNotNull { rad -> rad.identitetsnummer }}
                .distinct()
                .map { identitetsnummer -> Identitetsnummer(identitetsnummer) }
        ) {
            genererResponse(rader)
        }

    }

    private fun genererResponse(rader: List<Pair<UUID, List<Row<Any>>>>): TidslinjeResponse {
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
                    hendelseType = HendelseType.bekreftelseMinusV1,
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
        }
        return TidslinjeResponse(apiTidslinjer)
    }
}

/**
 * Mapper alt untatt bekreftelsemelding_v1 til Hendelse.
 * Bekreftelsemelding_v1 håndteres separat i lagTidslinjer.
 */
private fun mapIkkeBekreftelseRaderTilHendelser(rad: Row<Any>): Hendelse = when (rad.type) {
    periode_startet_v1 -> Hendelse(
        hendelseType = HendelseType.periode_startetMinusV1,
        tidspunkt = rad.timestamp,
        periodeStartetV1 = rad.data as Metadata
    )

    periode_avsluttet_v1 -> Hendelse(
        hendelseType = HendelseType.periode_avsluttetMinusV1,
        tidspunkt = rad.timestamp,
        periodeAvsluttetV1 = rad.data as Metadata
    )

    pa_vegne_av_start_v1 -> Hendelse(
        hendelseType = HendelseType.pa_vegne_av_startMinusV1,
        tidspunkt = rad.timestamp,
        paVegneAvStartV1 = rad.data as PaaVegneAvStart
    )

    pa_vegne_av_stopp_v1 -> Hendelse(
        hendelseType = HendelseType.pa_vegne_av_stoppMinusV1,
        tidspunkt = rad.timestamp,
        paVegneAvStoppV1 = rad.data as PaaVegneAvStopp
    )

    opplysninger_om_arbeidssoeker_v4 -> Hendelse(
        hendelseType = HendelseType.opplysningerMinusV4,
        tidspunkt = rad.timestamp,
        opplysningerV4 = rad.data as OpplysningerOmArbeidssoeker
    )

    profilering_v1 -> Hendelse(
        hendelseType = HendelseType.profileringMinusV1,
        tidspunkt = rad.timestamp,
        profileringV1 = rad.data as Profilering
    )

    else -> throw IllegalArgumentException("Ukjent hendelsestype: ${rad.type}")
}