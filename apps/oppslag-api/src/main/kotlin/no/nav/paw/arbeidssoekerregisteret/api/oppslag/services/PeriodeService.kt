package no.nav.paw.arbeidssoekerregisteret.api.oppslag.services

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ArbeidssoekerperiodeAggregertResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ArbeidssoekerperiodeResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Paging
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.toArbeidssoekerPeriodeAggregertResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.toArbeidssoekerperiodeResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.toBekreftelseResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.toOpplysningerOmArbeidssoekerAggregertResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.toPeriode
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.toProfileringResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.BekreftelseRepository
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.OpplysningerRepository
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.PeriodeRepository
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.ProfileringRepository
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.buildLogger
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.model.Identitetsnummer
import org.apache.kafka.clients.consumer.ConsumerRecords
import java.util.*

class PeriodeService(
    private val periodeRepository: PeriodeRepository,
    private val opplysningerRepository: OpplysningerRepository,
    private val profileringRepository: ProfileringRepository,
    private val bekreftelseRepository: BekreftelseRepository
) {
    private val logger = buildLogger

    fun hentPeriodeForId(periodeId: UUID): Periode? = periodeRepository.hentPeriodeForId(periodeId)?.toPeriode()

    fun finnPerioderForIdentiteter(
        identitetsnummerList: Collection<Identitetsnummer>,
        paging: Paging = Paging()
    ): List<ArbeidssoekerperiodeResponse> =
        periodeRepository.finnPerioderForIdentiteter(identitetsnummerList, paging)
            .map { it.toArbeidssoekerperiodeResponse() }

    fun finnAggregertePerioderForIdenter(
        identitetsnummerList: Collection<Identitetsnummer>,
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

    fun lagreAllePerioder(perioder: Iterable<Periode>) =
        periodeRepository.lagreAllePerioder(perioder)

    fun handleRecords(records: ConsumerRecords<Long, Periode>) =
        lagreAllePerioder(records.map { it.value() })
}
