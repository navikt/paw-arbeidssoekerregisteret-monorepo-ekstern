package no.nav.paw.arbeidssoekerregisteret.api.oppslag.services

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.*
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.BekreftelseRepository
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.OpplysningerRepository
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.PeriodeRepository
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.ProfileringRepository
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import java.util.*

class PeriodeService(
    private val periodeRepository: PeriodeRepository,
    private val opplysningerRepository: OpplysningerRepository,
    private val profileringRepository: ProfileringRepository,
    private val bekreftelseRepository: BekreftelseRepository
) {
    fun hentPeriodeForId(periodeId: UUID): Periode? = periodeRepository.hentPeriodeForId(periodeId)?.toPeriode()

    fun finnPerioderForIdentiteter(
        identitetsnummerList: List<Identitetsnummer>,
        paging: Paging = Paging()
    ): List<ArbeidssoekerperiodeResponse> =
        periodeRepository.finnPerioderForIdentiteter(identitetsnummerList, paging)
            .map { it.toArbeidssoekerperiodeResponse() }

    fun finnAggregertePerioderForIdenter(
        identitetsnummerList: List<Identitetsnummer>,
        paging: Paging = Paging()
    ): List<ArbeidssoekerperiodeAggregertResponse> =
        periodeRepository.finnPerioderForIdentiteter(identitetsnummerList, paging).map { periode ->
            val opplysningerAggregert =
                opplysningerRepository.finnOpplysningerForPeriodeIdList(listOf(periode.periodeId)).map { opplysning ->
                    val profilering = profileringRepository.hentProfileringForPeriodeIdOgOpplysningerId(
                        periode.periodeId,
                        opplysning.opplysningerId
                    )
                    opplysning.toOpplysningerOmArbeidssoekerAggregertResponse(profilering?.toProfileringResponse())
                }

            val bekreftelser = bekreftelseRepository.finnBekreftelserForPeriodeIdList(listOf(periode.periodeId))
                .map { it.toBekreftelseResponse() }

            periode.toArbeidssoekerPeriodeAggregertResponse(opplysningerAggregert, bekreftelser)
        }

    fun lagreAllePerioder(perioder: Sequence<Periode>) = periodeRepository.lagreAllePerioder(perioder)
}
