package no.naw.paw.minestillinger.brukerprofil

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.error.model.Data
import no.nav.paw.error.model.Response
import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.pdl.client.PdlClient
import no.naw.paw.minestillinger.Clock
import no.naw.paw.minestillinger.appLogger
import no.naw.paw.minestillinger.brukerIkkeFunnet
import no.naw.paw.minestillinger.brukerprofil.beskyttetadresse.AdressebeskyttelseFeil
import no.naw.paw.minestillinger.brukerprofil.beskyttetadresse.AdressebeskyttelseVerdi
import no.naw.paw.minestillinger.brukerprofil.beskyttetadresse.harBeskyttetAdresse
import no.naw.paw.minestillinger.brukerprofil.beskyttetadresse.harBeskyttetAdresseBulk
import no.naw.paw.minestillinger.brukerprofil.flagg.Flagg
import no.naw.paw.minestillinger.brukerprofil.flagg.HarBeskyttetadresseFlagg
import no.naw.paw.minestillinger.brukerprofil.flagg.HarGodeMuligheterFlagg
import no.naw.paw.minestillinger.brukerprofil.flagg.HarGodeMuligheterFlaggtype
import no.naw.paw.minestillinger.brukerprofil.flagg.LagretFlagg
import no.naw.paw.minestillinger.brukerprofil.flagg.ListeMedFlagg
import no.naw.paw.minestillinger.brukerprofil.flagg.OppdateringAvFlagg
import no.naw.paw.minestillinger.brukerprofil.flagg.TjenestenErAktivFlagg
import no.naw.paw.minestillinger.domain.BrukerId
import no.naw.paw.minestillinger.domain.BrukerProfil
import no.naw.paw.minestillinger.domain.BrukerProfilerUtenFlagg
import no.naw.paw.minestillinger.domain.PeriodeId
import no.naw.paw.minestillinger.domain.Profilering
import no.naw.paw.minestillinger.domain.ProfileringResultat
import no.naw.paw.minestillinger.domain.medFlagg
import java.time.Instant

class BrukerprofilTjeneste(
    val meterRegistry: MeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
    val pdlClient: PdlClient,
    val hentBrukerprofilUtenFlagg: (Identitetsnummer) -> BrukerProfilerUtenFlagg?,
    val skrivFlagg: (BrukerId, Iterable<LagretFlagg>) -> Unit,
    val hentFlagg: (BrukerId) -> List<Flagg>,
    val hentProfilering: (PeriodeId) -> Profilering?,
    val slettAlleSøk: (BrukerId) -> Unit,
    val clock: Clock
) {
    fun hentLokalBrukerprofil(identitetsnummer: Identitetsnummer): Response<BrukerProfil> {
        return hentLokalBrukerProfilEllerNull(identitetsnummer)
            ?.let(::Data)
            ?: brukerIkkeFunnet()
    }

    fun hentLokalBrukerProfilEllerNull(identitetsnummer: Identitetsnummer): BrukerProfil? {
        val brukerProfilerUtenFlagg = hentBrukerprofilUtenFlagg(identitetsnummer) ?: return null
        val profileringsFlagg = genererProfileringsFlagg(brukerProfilerUtenFlagg.arbeidssoekerperiodeId)
        val flaggFraDatabasen = hentFlagg(brukerProfilerUtenFlagg.id)
        val gjeldeneFlagg = ListeMedFlagg.listeMedFlagg(flaggFraDatabasen) + profileringsFlagg
        return brukerProfilerUtenFlagg.medFlagg(gjeldeneFlagg)
    }

    @WithSpan("vedlikehold_oppdater_adresse_gradering_bulk")
    suspend fun oppdaterAdresseGraderingBulk(
        brukerprofiler: List<BrukerProfil>,
        tidspunkt: Instant
    ) {
        if (brukerprofiler.isEmpty()) return
        val map = brukerprofiler.associateBy(BrukerProfil::identitetsnummer)
        pdlClient.harBeskyttetAdresseBulk(brukerprofiler.map(BrukerProfil::identitetsnummer))
            .forEach { beskyttetAdresse ->
                when (beskyttetAdresse) {
                    is AdressebeskyttelseFeil -> {
                        appLogger.error("Feil ved henting av adressebeskyttelse, code=${beskyttetAdresse.code}")
                        Span.current().addEvent("pdl_error", Attributes.of(stringKey("code"), beskyttetAdresse.code))
                    }
                    is AdressebeskyttelseVerdi -> {
                        val brukerProfil = map[beskyttetAdresse.identitetsnummer]
                        if (brukerProfil != null) {
                            oppdaterAdresseGradering(
                                brukerProfil = brukerProfil,
                                tidspunkt = tidspunkt,
                                harGradertAdresseNå = beskyttetAdresse.harBeskyttetAdresse
                            )
                        } else {
                            appLogger.warn("Uventet identitsnummer i adressebeskyttelse resultat")
                        }
                    }
                }
            }
    }

    suspend fun oppdaterAdresseGradering(
        brukerProfil: BrukerProfil,
        tidspunkt: Instant
    ): BrukerProfil {
        val harGradertAdresseNå = pdlClient.harBeskyttetAdresse(brukerProfil.identitetsnummer)
        return oppdaterAdresseGradering(brukerProfil, tidspunkt, harGradertAdresseNå)
    }

    fun oppdaterAdresseGradering(
        brukerProfil: BrukerProfil,
        tidspunkt: Instant,
        harGradertAdresseNå: Boolean
    ): BrukerProfil {
        val gradertAdresseFlagg = HarBeskyttetadresseFlagg(
            verdi = harGradertAdresseNå,
            tidspunkt = tidspunkt
        )
        val flagg = brukerProfil.listeMedFlagg
            .replace(gradertAdresseFlagg)
            .let { flagg ->
                if (harGradertAdresseNå) {
                    flagg.addOrUpdate(
                        TjenestenErAktivFlagg(
                            verdi = false,
                            tidspunkt = tidspunkt
                        )
                    )
                } else flagg
            }
        oppdaterFlagg(
            brukerProfil.id, OppdateringAvFlagg(
                nyeOgOppdaterteFlagg = flagg.flaggSomMåOppdateres.toList(),
                søkSkalSlettes = harGradertAdresseNå
            )
        )
        return brukerProfil.copy(listeMedFlagg = flagg.clean())
    }

    fun genererProfileringsFlagg(periodeId: PeriodeId): HarGodeMuligheterFlagg {
        return hentProfilering(periodeId)
            ?.let { profilering ->
                val godeMuligheter = profilering.profileringResultat == ProfileringResultat.ANTATT_GODE_MULIGHETER
                HarGodeMuligheterFlaggtype.flagg(verdi = godeMuligheter, tidspunkt = profilering.profileringTidspunkt)
            } ?: HarGodeMuligheterFlaggtype.flagg(verdi = false, tidspunkt = Instant.EPOCH)
    }

    fun oppdaterFlagg(brukerId: BrukerId, oppdatering: OppdateringAvFlagg) {
        skrivFlagg(brukerId, oppdatering.nyeOgOppdaterteFlagg)
        if (oppdatering.søkSkalSlettes) {
            slettAlleSøk(brukerId)
        }
    }
}
