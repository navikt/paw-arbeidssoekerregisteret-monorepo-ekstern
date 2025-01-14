package no.nav.paw.arbeidssoekerregisteret.api.oppslag.services

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.OpplysningerOmArbeidssoekerResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Paging
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.toOpplysningerOmArbeidssoekerResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.OpplysningerRepository
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.model.Identitetsnummer
import org.apache.kafka.clients.consumer.ConsumerRecords
import java.util.*

class OpplysningerService(private val opplysningerRepository: OpplysningerRepository) {

    fun finnOpplysningerForPeriodeIdList(
        periodeIdList: Collection<UUID>,
        paging: Paging = Paging()
    ): List<OpplysningerOmArbeidssoekerResponse> =
        opplysningerRepository.finnOpplysningerForPeriodeIdList(periodeIdList, paging)
            .map { it.toOpplysningerOmArbeidssoekerResponse() }

    fun finnOpplysningerForIdentiteter(
        identitetsnummerList: Collection<Identitetsnummer>,
        paging: Paging = Paging()
    ): List<OpplysningerOmArbeidssoekerResponse> =
        opplysningerRepository.finnOpplysningerForIdentiteter(identitetsnummerList, paging)
            .map { it.toOpplysningerOmArbeidssoekerResponse() }

    fun lagreAlleOpplysninger(opplysninger: Iterable<OpplysningerOmArbeidssoeker>) =
        opplysningerRepository.lagreAlleOpplysninger(opplysninger)

    fun handleRecords(records: ConsumerRecords<Long, OpplysningerOmArbeidssoeker>) =
        lagreAlleOpplysninger(records.map { it.value() })
}
