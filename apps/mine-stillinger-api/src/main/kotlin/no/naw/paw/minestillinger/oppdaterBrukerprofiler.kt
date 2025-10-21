package no.naw.paw.minestillinger

import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import no.nav.paw.pdl.client.PdlClient
import no.naw.paw.minestillinger.db.ops.hentUtdaterteBrukerprofiler
import no.naw.paw.minestillinger.domain.BrukerProfil
import no.naw.paw.minestillinger.domain.KanTilbysTjenesten
import no.naw.paw.minestillinger.domain.KanTilbysTjenesten.JA
import no.naw.paw.minestillinger.domain.KanTilbysTjenesten.NEI
import no.naw.paw.minestillinger.domain.KanTilbysTjenesten.UKJENT
import no.naw.paw.minestillinger.domain.TjenesteStatus
import no.naw.paw.minestillinger.domain.TjenesteStatus.AKTIV
import no.naw.paw.minestillinger.domain.TjenesteStatus.INAKTIV
import java.time.Duration
import java.time.Instant

suspend fun oppdaterBrukerprofiler(
    pdlClient: PdlClient,
    tidspunkt: Instant,
    oppdaterAlleEldreEnn: Duration,
    antallPerTx: Int
) {
    val sistOppdatertFør = tidspunkt - oppdaterAlleEldreEnn
    val profiler = hentUtdaterteBrukerprofiler(
        kanTilbysTjenesteOppdatertFør = sistOppdatertFør,
        maksAntall = antallPerTx
    )
    val resultat = pdlClient.harBeskyttetAdresseBulk(profiler.map { it.identitetsnummer })
}

fun oppdaterBrukerprofiler(
    resultat: List<AdressebeskyttelseResultat>,
    profiler: List<BrukerProfil>,
    tidspunkt: Instant
): List<BrukerProfil> {
    val pdlResultatMap = resultat.associateBy(AdressebeskyttelseResultat::identitetsnummer)
    profiler.map { gjeldeneProfil ->
        val tjenestenKanLeveres = pdlResultatMap[gjeldeneProfil.identitetsnummer]
            .also { pdlRes ->
                when (pdlRes) {
                    is AdressebeskyttelseFeil -> {
                        Span.current().addEvent("paw_mine_stillinger_adressebeskyttelse_feil", Attributes.of(
                            stringKey("kode"), pdlRes.code
                        ))
                        UKJENT
                    }
                    is AdressebeskyttelseVerdi -> {
                        if (pdlRes.harBeskyttetAdresse) NEI else KanTilbysTjenesten.JA
                    }
                    null -> {
                        Span.current().addEvent("paw_mine_stillinger_adressebeskyttelse_mangler")
                        UKJENT
                    }
                }
            }

    }
    TODO()

}

fun tjenestenKanIkkeLeveres(brukerProfil: BrukerProfil, tidspunkt: Instant): BrukerProfil {
    val oppdatertProfil = brukerProfil.copy(
        kanTilbysTjenesten = NEI,
        kanTilbysTjenestenTimestamp = tidspunkt,
        tjenestestatus = TjenesteStatus.KAN_IKKE_LEVERES
    )
    Span.current().addEvent("paw_mine_stillinger_tjeneste_kan_ikke_leveres", Attributes.of(
        stringKey("gjeldene_tjeneste_status"), brukerProfil.tjenestestatus.name
    ))
    return oppdatertProfil
}

fun ukjentTjenesteStatus(brukerProfil: BrukerProfil, tidspunkt: Instant): BrukerProfil {
    val (nyTjenestestatus, nyKanTilbysTjenesten) = if (brukerProfil.tjenestestatus == AKTIV) {
        AKTIV to JA
    } else {
        INAKTIV to UKJENT
    }
    val oppdatertProfil = brukerProfil.copy(
        kanTilbysTjenesten = nyKanTilbysTjenesten,
        )
    Span.current().addEvent("paw_mine_stillinger_ukjent_tjeneste_status", Attributes.of(
        stringKey("gjeldene_tjeneste_status"), brukerProfil.tjenestestatus.name
    ))
    TODO()
    //return oppdatertProfil
}

fun tjenestenKanLeveres(brukerProfil: BrukerProfil, tidspunkt: Instant): BrukerProfil {
    val oppdatertProfil = brukerProfil.copy(
        kanTilbysTjenesten = JA,
        kanTilbysTjenestenTimestamp = tidspunkt,
        tjenestestatus = if (brukerProfil.tjenestestatus == TjenesteStatus.KAN_IKKE_LEVERES) {
            Span.current().addEvent("paw_mine_stillinger_kan_naa_leveres")
            INAKTIV
        } else {
            brukerProfil.tjenestestatus
        }
    )
    return oppdatertProfil
}


